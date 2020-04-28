package com.qf.v6.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qf.cart.api.ICartService;
import com.qf.constant.CookieConstant;
import com.qf.dto.ResultBean;
import com.qf.entity.TUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
@RequestMapping("cart")
public class CartController {

    @Reference
    private ICartService cartService;

    /**
     * 添加商品到购物车
     *
     * 	1）当前用户没有购物车
     * 		新建购物车，把商品添加到购物车中，再把购物车存到redis中
     * 	2）当前用户有购物车，但是购物车中没有该商品
     * 		先从redis中获取该购物车，再把商品添加都购物车中，再存入到redis中。
     * 	3）当前用户有购物车，且购物车中有该商品
     * 		先从redis中获取该购物车，再获取该商品的数量，再让新的数量和老的数量相加，更新回购物车中，再更新回redis中。
     *
     * @param productId  这一次要添加到购物车的商品的id
     * @param count   数量
     * @return
     */
    @RequestMapping("add/{productId}/{count}")
    @ResponseBody
    public ResultBean addProduct(@CookieValue(name = CookieConstant.USER_CART, required = false) String uuid,
                                 @PathVariable Long productId,
                                 @PathVariable int count,
                                 HttpServletResponse response,
                                 HttpServletRequest request){



        Object o = request.getAttribute("user");
        if(o!=null){
            //================已登录状态下的购物车======================= redis:    user:cart:userId
            TUser user = (TUser) o;
            Long userId = user.getId();
            return cartService.addProduct(userId.toString(),productId,count);

        }


        //==============未登录状态下的购物车=================
        //把该商品添加到购物车，这个购物车是在redis中。

        if(uuid==null||"".equals(uuid)){
            //uuid为空的话再生成一个uuid放到cookie里给客户端
            uuid = UUID.randomUUID().toString();
            //返回该uuid给cookie
            Cookie cookie = new Cookie(CookieConstant.USER_CART,uuid);
            cookie.setPath("/");
            response.addCookie(cookie);
        }
        ResultBean resultBean = cartService.addProduct(uuid,productId,count);

        return resultBean;
    }


    /**
     * 清空购物车
     * @param uuid
     * @param response
     * @return
     */
    @RequestMapping("clean")
    @ResponseBody
    public ResultBean cleanCart(@CookieValue(name=CookieConstant.USER_CART,required = false)String uuid,HttpServletResponse response,
                                HttpServletRequest request){
        Object o = request.getAttribute("user");
        if(o!=null){
            //===========已登录状态下的购物车============
            TUser user = (TUser) o;
            return cartService.clean(user.getId().toString());
        }
        //===========未登录状态下的购物车===========
        if(uuid!=null&&!"".equals(uuid)){
            //删除Cookie
            Cookie cookie = new Cookie(CookieConstant.USER_CART,"");
            cookie.setMaxAge(0);
            cookie.setPath("/");//admin.qf.com  sso.qf.com  ****.qf.com
            // cookie.setDomain("qf.com");//如果我们使用域名来访问，那么cookie不会被携带，只有这边设置了这个一级域名，qf.com,那么在该域名下的所有二级cookie就都可以携带该cookie
            response.addCookie(cookie);

            //删除redis中的购物车
            return cartService.clean(uuid);
        }
        return ResultBean.error("当前用户没有购物车");

    }

    /**
     * 更新购物车中的商品
     * @param productId
     * @param count
     * @param uuid
     * @return
     */
    @RequestMapping("update/{productId}/{count}")
    @ResponseBody
    public ResultBean updateCart(
            @PathVariable Long productId,
            @PathVariable int count,
            @CookieValue(name=CookieConstant.USER_CART,required = false)String uuid,
            HttpServletRequest request
    ){

        Object o = request.getAttribute("user");
        if(o!=null){
            //=============已登录状态下的购物车==============  user:cart:userId
            TUser user = (TUser) o;
            return cartService.update(user.getId().toString(),productId,count);

        }

        return cartService.update(uuid,productId,count);

    }


    /**
     * 展示购物车
     * @param uuid
     * @param request
     * @return
     */
    @RequestMapping("show")
    @ResponseBody
    public ResultBean showCart(@CookieValue(name=CookieConstant.USER_CART,required = false)String uuid,HttpServletRequest request){

        Object o = request.getAttribute("user");
        //============查看已登录状态下的购物车=============
        if(o!=null){
            TUser user = (TUser) o;
            Long userId = user.getId();
            return cartService.showCart(userId.toString());
        }


        //============查看未登录状态下的购物车=============
        return cartService.showCart(uuid);

    }

    /**
     * 合并两种状态下的购物车
     * @param uuid
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("merge")
    @ResponseBody
    public ResultBean merge(@CookieValue(name = CookieConstant.USER_CART,required = false)String uuid,
                            HttpServletRequest request,HttpServletResponse response){
        //获得uuid,和uid
        TUser user = (TUser) request.getAttribute("user");
        String userId = null;
        if(user!=null){
            userId = user.getId().toString();
        }


        //做完合并以后，要把未登录状态下的购物车清空。在清空cookie
        Cookie cookie = new Cookie(CookieConstant.USER_CART,"");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);


        return cartService.merge(uuid,userId);

    }


}
