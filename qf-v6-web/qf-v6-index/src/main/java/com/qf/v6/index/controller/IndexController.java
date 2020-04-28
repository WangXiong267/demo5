package com.qf.v6.index.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.qf.constant.CookieConstant;
import com.qf.dto.ResultBean;
import com.qf.entity.TProductType;
import com.qf.product.type.api.IProductTypeService;
import com.qf.util.HttpClientUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {


    @Reference
    private IProductTypeService productTypeService;


    @RequestMapping({"","index"})
    public String showIndex(Model model) {

        List<TProductType> types = productTypeService.selectAll();
        model.addAttribute("types",types);
        return "index";
    }

    @RequestMapping("checkIsLogin")
    @ResponseBody
    // HttpServletRequest对象中获取Cookie的键是“user_login”的那个值，赋值给uuid,根据uuid组织好redis中键 直接访问redis
    public ResultBean checkIsLogin(@CookieValue(name= CookieConstant.USER_LOGIN,required = false)String uuid){

        //来访问sso提供的checkIsLogin接口==》得到resultBean
        //1.使用HttpClient工具类来发送请求
        String url = "http://localhost:9083/user/checkIsLogin";
        //dbe06afc-8540-4b74-8035-99e188d33933
        String cookie = new StringBuilder().append(CookieConstant.USER_LOGIN).append("=").append(uuid).toString();
        // user_login=dbe06afc-8540-4b74-8035-99e188d33933
        String result = HttpClientUtils.doGet(url, cookie); //result是一个json字符串

        //把result字符串封装成ResultBean对象
        //jackjson
        Gson gson = new Gson();
        ResultBean resultBean = gson.fromJson(result, ResultBean.class);
        return resultBean;
    }



}
