public class DubboTagAdvice {
    @Advice.OnMethodEnter
    static long enter(@Advice.AllArguments Object args[], @Advice.Origin Method method){
        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit
    static void exit(@Advice.Enter long startTime,
                     @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object result,
                     @Advice.Origin Method method,
                     @Advice.Thrown Throwable throwable){
        if(throwable != null){
            System.out.println("error func " + System.currentTimeMillis());
        }else {
            System.out.println("func takes " + (System.currentTimeMillis() - startTime));
        }
    }
}
