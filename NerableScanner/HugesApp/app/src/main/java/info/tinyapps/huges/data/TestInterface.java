package info.tinyapps.huges.data;

import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

/**
 * aws backend call interface
 */
public interface TestInterface {
    @LambdaFunction
    ResponseData writeFactsToDB(RequestData nameInfo);
}

