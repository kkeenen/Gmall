package com.atguigu.gulimall.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.feign.MemberFeignService;
import com.atguigu.gulimall.vo.UserLoginVo;
import com.atguigu.gulimall.vo.UserRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    MemberFeignService memberFeignService;

    @PostMapping("/register")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){
            /**
             result.getFieldErrors().stream().map(fieldError -> {String field = fieldError.getField();
             *                 String defaultMessage = fieldError.getDefaultMessage();
             *                 errors.put(field, defaultMessage);
             *             })
             */
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> {
                return fieldError.getField();
            }, fieldError -> {
                return fieldError.getDefaultMessage();
            }));
            redirectAttributes.addFlashAttribute("errors", errors);
            // 校验出错，转发到注册页
            return "redirect:/reg.html";
        }

        // 真正注册 调用远程服务

        R r = memberFeignService.regist(vo);
        if(r.getCode()==0){
            // 成功
            return "redirect:auth.dns19.hichina.com/login.html";
        }else{
            // 失败
            Map<String, String> errors = new HashMap<>();
            return "redirect:/reg.html";
        }

    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes){

        // 远程登陆
        R login = memberFeignService.login(vo);
        if(login.getCode()==0){

            return "redirect:http://dns19.hichina.com";
        }else{

            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);


            return "redirect:http://auth.dns19.hichina.com/login.html";

        }
    }


}
