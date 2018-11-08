package org.apache.dubbo.test;

/**
 * <p>
 * 类说明
 * </p>
 *
 * @author Shawn
 * @since 2018/11/8
 */
public class CodeBlockDemo
{
    {
        System.out.println("初始化代码");
    }

    CodeBlockDemo()
    {
        System.out.println("构造器");
    }

    static
    {
        System.out.println("静态代码块");
    }

    //运行后输出结果?
    public static void main(String[] args)
    {
        {
            int a = 10;
            //10
            System.out.println("局部代码块");
        }

        new CodeBlockDemo();
        new CodeBlockDemo();
        new CodeBlockDemo();
    }
}

