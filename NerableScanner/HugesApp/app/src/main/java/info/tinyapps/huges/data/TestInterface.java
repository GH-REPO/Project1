package info.tinyapps.huges.data;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

public interface TestInterface {
    @LambdaFunction
    ResponseData writeFactsToDB(RequestData nameInfo);
}

