package com.atguigu.gmall.cart.exception;

import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 收到所有controller的异常
 */
//@ResponseBody
//@ControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 业务期间出现的所有异常都用 GmallException 包装
     * @param gmallException
     * @return
     */
    @ExceptionHandler(GmallException.class)
    public Result handleGmallException(GmallException gmallException){

        return Result.ok(); //给前端的返回
    }
}
