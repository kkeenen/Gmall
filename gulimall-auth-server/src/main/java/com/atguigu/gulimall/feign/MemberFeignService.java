package com.atguigu.gulimall.feign;


import com.atguigu.common.utils.R;
import com.atguigu.gulimall.vo.UserLoginVo;
import com.atguigu.gulimall.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);


}
