package com.shanyangcode.userservice.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shanyangcode.common.common.ErrorCode;
import com.shanyangcode.common.constant.CommonConstant;
import com.shanyangcode.common.constant.SessionTypeConstant;
import com.shanyangcode.common.exception.ThrowUtils;
import com.shanyangcode.common.model.vo.UserInfosResponse;
import com.shanyangcode.common.utils.JwtUtil;
import com.shanyangcode.common.utils.SnowflakeUtil;
import com.shanyangcode.userservice.constant.UserConstant;
import com.shanyangcode.userservice.loadbalancer.NettyServiceLocator;
import com.shanyangcode.userservice.mapper.UserMapper;
import com.shanyangcode.userservice.model.dto.UpdateAvatarRequest;
import com.shanyangcode.userservice.model.dto.UserLoginCodeRequest;
import com.shanyangcode.userservice.model.dto.UserLoginPasswordRequest;
import com.shanyangcode.userservice.model.dto.UserRegisterRequest;
import com.shanyangcode.userservice.model.entity.Session;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.entity.UserSession;
import com.shanyangcode.userservice.model.vo.LoginAndRegisterResponse;
import com.shanyangcode.userservice.model.vo.TokenResponse;
import com.shanyangcode.userservice.model.vo.UploadUrlResponse;
import com.shanyangcode.userservice.service.SessionService;
import com.shanyangcode.userservice.service.UserService;
import com.shanyangcode.userservice.service.UserSessionService;
import com.shanyangcode.userservice.utils.EmailUtil;
import com.shanyangcode.userservice.utils.OssUtils;
import com.shanyangcode.userservice.utils.RandomCodeUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {


    @Resource
    private NettyServiceLocator nettyServiceLocator;

    @Resource
    private EmailUtil emailUtil;


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OssUtils ossUtils;

    @Resource
    private UserSessionService userSessionService;

    @Resource
    private SessionService sessionService;

    @Override
    public void sendCaptcha(String targetEmail) {
        String existingCode = stringRedisTemplate.opsForValue().get(targetEmail);
        ThrowUtils.throwIf(StringUtils.isNotBlank(existingCode), ErrorCode.LOGIN_SEND_CODE_ERROR);

        String randomCode = RandomCodeUtil.getRandomCode();
        emailUtil.sendEmail(targetEmail, randomCode);
        stringRedisTemplate.opsForValue().set(targetEmail, randomCode, UserConstant.CAPTCHA_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    @Override
    public LoginAndRegisterResponse register(UserRegisterRequest userRegisterRequest) {

        String email = userRegisterRequest.getEmail();
        String code = userRegisterRequest.getCode();
        // 验证验证码是否正确
        String redisCode = stringRedisTemplate.opsForValue().get(email);
        ThrowUtils.throwIf(StringUtils.isBlank(redisCode) || !code.equals(redisCode), ErrorCode.LOGIN_ERROR_CODE);

        // 验证用户账号是否已经存在
        ThrowUtils.throwIf(getUser(email) != null, ErrorCode.USER_ALREADY_EXISTS);


        // 验证密码是否相同
        ThrowUtils.throwIf(!userRegisterRequest.getPassword().equals(userRegisterRequest.getConfirmPassword()), ErrorCode.LOGIN_ERROR);


        String password = userRegisterRequest.getPassword();
        String encryptedPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + password).getBytes());


        LoginAndRegisterResponse loginAndRegisterResponse = new LoginAndRegisterResponse();
        Long userId = SnowflakeUtil.nextId();
        synchronized (email.intern()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUserId(userId);
            newUser.setNickname(userRegisterRequest.getNickname());
            newUser.setPassword(encryptedPassword);
            boolean saveUser = this.save(newUser);
            ThrowUtils.throwIf(!saveUser, ErrorCode.SYSTEM_ERROR);
            BeanUtil.copyProperties(getUser(email), loginAndRegisterResponse);
        }

        Long sessionId = SnowflakeUtil.nextId();
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setStatus(CommonConstant.SESSION_STATUS);
        session.setType(SessionTypeConstant.ROBOT_TYPE);
        ThrowUtils.throwIf(!sessionService.save(session), ErrorCode.SYSTEM_ERROR);

        // 创建用户会话（普通用户）
        UserSession userSessionUser=createUserSession(userId,sessionId,CommonConstant.USER_ROLE_NORMAL,CommonConstant.SESSION_STATUS);

        //创建AI会话
        UserSession userSessionAI=createUserSession(CommonConstant.AI_ID,sessionId,CommonConstant.USER_ROLE_NORMAL,CommonConstant.SESSION_STATUS);

        List<UserSession> sessionList= Arrays.asList(userSessionUser,userSessionAI);

        ThrowUtils.throwIf(!userSessionService.saveBatch(sessionList), ErrorCode.SYSTEM_ERROR);

        stringRedisTemplate.delete(email);
        return createJwt(loginAndRegisterResponse);
    }

    public UserSession createUserSession(Long userId,Long sessionId,Integer role,Integer status) {
        UserSession userSession=new UserSession();
        userSession.setUserId(userId);
        userSession.setSessionId(sessionId);
        userSession.setRole(role);
        userSession.setStatus(status);
        return userSession;
    }

    @Override
    public LoginAndRegisterResponse loginPassword(UserLoginPasswordRequest userLoginPasswordRequest) {
        String email = userLoginPasswordRequest.getEmail();
        String password = userLoginPasswordRequest.getPassword();

        User user = getUser(email);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);

        String encryptedPassword = DigestUtils.md5DigestAsHex((UserConstant.PASSWORD_SALT + password).getBytes());
        ThrowUtils.throwIf(!encryptedPassword.equals(user.getPassword()), ErrorCode.LOGIN_ERROR);

        LoginAndRegisterResponse loginAndRegisterResponse = new LoginAndRegisterResponse();
        BeanUtil.copyProperties(user, loginAndRegisterResponse);
        return createJwt(loginAndRegisterResponse);

    }

    @Override
    public LoginAndRegisterResponse loginCode(UserLoginCodeRequest userLoginCodeRequest) {
        String email = userLoginCodeRequest.getEmail();
        String code = userLoginCodeRequest.getCode();

        String redisCode = stringRedisTemplate.opsForValue().get(email);
        ThrowUtils.throwIf(StringUtils.isBlank(redisCode) || !code.equals(redisCode), ErrorCode.LOGIN_ERROR_CODE);

        // 删除 redis 保存的验证码
        stringRedisTemplate.delete(email);

        User user = getUser(email);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXISTS);

        LoginAndRegisterResponse loginAndRegisterResponse = new LoginAndRegisterResponse();
        BeanUtil.copyProperties(user, loginAndRegisterResponse);

        return createJwt(loginAndRegisterResponse);
    }

    public User getUser(String email) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        return this.getOne(queryWrapper);
    }

    public LoginAndRegisterResponse createJwt(LoginAndRegisterResponse loginAndRegisterResponse) {
        String userId = loginAndRegisterResponse.getUserId().toString();
        String accessToken = JwtUtil.generate(userId, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
        String refreshToken = JwtUtil.generate(userId, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
        loginAndRegisterResponse.setAccessToken(accessToken);
        loginAndRegisterResponse.setRefreshToken(refreshToken);
        stringRedisTemplate.opsForValue().set(CommonConstant.ACCESS_TOKEN_PREFIX + userId, accessToken, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
        stringRedisTemplate.opsForValue().set(CommonConstant.REFRESH_TOKEN_PREFIX + userId, refreshToken, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
        String nettyUri = nettyServiceLocator.getServiceInstance(loginAndRegisterResponse.getUserId().toString());
        loginAndRegisterResponse.setNettyUri(nettyUri);

        //获取并清除离线时间
        Long offlineTime=getAndClearOfflineTime(userId);
        loginAndRegisterResponse.setOfflineTime(offlineTime);
        return loginAndRegisterResponse;
    }

    private Long getAndClearOfflineTime(String userId) {
        String key=CommonConstant.OFFLINE_KEY_REDIS+userId;
        String value = stringRedisTemplate.opsForValue().getAndDelete(key);
        if(StringUtils.isNotBlank(value)) {
            log.info("用户 {} 上线，离线时间: {}", userId, value);
            return Long.parseLong(value);
        }

        //新用户首次登陆没有离线记录
        log.debug("用户 {} 无离线时间记录", userId);
        return null;
    }

    @Override
    public boolean logout(String userId) {
        stringRedisTemplate.delete(CommonConstant.ACCESS_TOKEN_PREFIX + userId);
        stringRedisTemplate.delete(CommonConstant.REFRESH_TOKEN_PREFIX + userId);
        return true;
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        // 1. 解析传入的 Refresh Token
        Claims claims = JwtUtil.parse(refreshToken);
        ThrowUtils.throwIf(claims == null, ErrorCode.TOKEN_INVALID, "凭证已失效，请重新登录");


        // 2. 从载荷中安全获取 userId
        String userId = claims.getSubject();

        // 3. 校验 Redis，防止 Token 撤销攻击（实现单设备登录的关键）
        String redisRefreshToken = stringRedisTemplate.opsForValue().get(CommonConstant.REFRESH_TOKEN_PREFIX + userId);
        ThrowUtils.throwIf(!refreshToken.equals(redisRefreshToken), ErrorCode.TOKEN_INVALID, "凭证已过期或在其他地方登录");


        // 4. 生成新的一对 Token
        String newAccessToken = JwtUtil.generate(userId, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
        String newRefreshToken = JwtUtil.generate(userId, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);

        // 5. 更新 Redis
        stringRedisTemplate.opsForValue().set(CommonConstant.ACCESS_TOKEN_PREFIX + userId, newAccessToken, CommonConstant.ACCESS_TOKEN_EXPIRE_TIME, CommonConstant.ACCESS_TOKEN_UNIT);
        stringRedisTemplate.opsForValue().set(CommonConstant.REFRESH_TOKEN_PREFIX + userId, newRefreshToken, CommonConstant.REFRESH_TOKEN_EXPIRE_TIME, CommonConstant.REFRESH_TOKEN_UNIT);
        return TokenResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken).build();
    }

    @Override
    public String refreshUri(Long userId) {
        return nettyServiceLocator.getServiceInstance(String.valueOf(userId));
    }

    @Override
    public UploadUrlResponse uploadUrl(String fileName) {
        UploadUrlResponse uploadUrlResponse = new UploadUrlResponse();
        uploadUrlResponse.setUploadUrl(ossUtils.uploadUrl(CommonConstant.BUCKET_NAME,fileName,CommonConstant.PICTURE_EXPIRE_TIME));
        uploadUrlResponse.setDownloadUrl(ossUtils.downUrl(CommonConstant.BUCKET_NAME,fileName));
        return uploadUrlResponse;
    }

    @Override
    public Boolean updateAvatar(UpdateAvatarRequest updateAvatarRequest) {
        User user=this.getById(updateAvatarRequest.getUserId());
        if(user==null){
            return false;
        }
        user.setAvatar(updateAvatarRequest.getUri());
        return this.updateById(user);
    }

    @Override
    public Map<Long,String> getUserNickName(Long sessionId) {
        List<Long> userIds=userSessionService.getUserIdBySessionId(sessionId);
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.in("user_id",userIds);
        List<User> users=this.list(queryWrapper);
        return users.stream().collect(Collectors.toMap(User::getUserId,User::getNickname));
    }

    @Override
    public Map<Long, UserInfosResponse> getUserInfos(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, UserInfosResponse> userInfosResponses = new HashMap<>();

        // 使用 Lambda Wrapper
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(User::getUserId, userIds);
        List<User> users = this.list(queryWrapper);

        if (users != null && !users.isEmpty()) {
            users.forEach(user -> {
                UserInfosResponse userInfosResponse = new UserInfosResponse();
                BeanUtil.copyProperties(user, userInfosResponse);
                userInfosResponses.put(user.getUserId(), userInfosResponse);
            });
        }
        return userInfosResponses;
    }
}




