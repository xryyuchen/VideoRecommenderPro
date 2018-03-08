package com.yuchen.server.rest;

import com.yuchen.server.model.request.LoginUserRequest;
import com.yuchen.server.model.request.RegisterUserRequest;
import com.yuchen.server.model.request.UpdateUserGenresRequest;
import com.yuchen.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuchen
 * 用于处理user相关的动作
 */
@Controller
@RequestMapping("/rest/users")
public class UserRestApi {

    @Autowired
    private UserService userService;

    /**
     * 需要提供用户注册功能
     * 访问：uri:/rest/users/register?username=abc&password=abc
     * 返回：{success：true}
     * @param usrname
     * @param password
     * @param model
     * @return
     */
    @RequestMapping(path = "/register",produces = "application/json",method = RequestMethod.GET)
    @ResponseBody
    public Model registerUser(@RequestParam("username") String username,@RequestParam("password") String password,Model model){
        model.addAttribute("success",userService.registerUser(new RegisterUserRequest(username,password)));
        return model;
    }

    /**
     * 需要提供用户的登录功能
     * 访问：uri：/rest/users/login?username=abc&password=abc
     * 返回：{success：true}
     * @param username
     * @param password
     * @param model
     * @return
     */
    @RequestMapping(path = "/login",produces = "application/json",method = RequestMethod.GET)
    @ResponseBody
    public Model loginUser(@RequestParam("username") String username,@RequestParam("password") String password,Model model){
        model.addAttribute("success",userService.loginUser(new LoginUserRequest(username,password)));
        return model;
    }

    /**
     * 需要能够添加用户偏爱的影片类别
     * 访问：uri：/rest/users/genres?username=abc&genres=a|b|c
     * 返回：{success：true}
     * @param username
     * @param password
     * @param model
     */
    @RequestMapping(path="/genres",produces = "application/json",method = RequestMethod.GET)
    @ResponseBody
    public void addGenres(@RequestParam("username") String username,@RequestParam("genres") String genres,Model model){
        List<String> genresList = new ArrayList<>();
        for (String genre : genres.split("\\|"))
            genresList.add(genre);
        userService.updateUserGenres(new UpdateUserGenresRequest(username,genresList));
     }
}












