package com.atguigu.gmall.cart.exception;

import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
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
        //业务状态的枚举类
        ResultCodeEnum codeEnum = gmallException.getCodeEnum();
        Result<String> result = Result.build("", codeEnum);
        return result; //给前端的返回
    }
}
