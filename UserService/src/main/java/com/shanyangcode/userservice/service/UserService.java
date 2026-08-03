package com.shanyangcode.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shanyangcode.userservice.model.dto.UserLoginCodeRequest;
import com.shanyangcode.userservice.model.dto.UserLoginPasswordRequest;
import com.shanyangcode.userservice.model.dto.UserRegisterRequest;
import com.shanyangcode.userservice.model.entity.User;
import com.shanyangcode.userservice.model.vo.LoginAndRegisterResponse;
import com.shanyangcode.userservice.model.vo.TokenResponse;


public interface UserService extends IService<User> {

    void sendCaptcha(String targetEmail);


    LoginAndRegisterResponse register(UserRegisterRequest userRegisterRequest);


    LoginAndRegisterResponse loginPassword(UserLoginPasswordRequest userLoginPasswordRequest);



    LoginAndRegisterResponse loginCode(UserLoginCodeRequest userLoginCodeRequest);

    boolean logout(String userId);


    TokenResponse refreshToken(String refreshToken);

    String refreshUri(Long userId);
}
