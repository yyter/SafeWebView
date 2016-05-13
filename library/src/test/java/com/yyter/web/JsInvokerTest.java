package com.yyter.web;

import org.junit.Test;

/**
 * Created by liyang on 5/13/16.
 */
public class JsInvokerTest {
    @Test
    public void testParseOK() throws JsInvoker.SyntaxException {
        //ok
        JsInvoker.ParserResult pr = new JsInvoker.Parser("obj.call()").parse();
        System.out.println(pr.toString());

        //ok
        pr = new JsInvoker.Parser("obj.call(abc, def, 123)").parse();
        System.out.println(pr.toString());

        //ok
        pr = new JsInvoker.Parser("obj.call(abc\n, def, 123);").parse();
        System.out.println(pr.toString());
    }

    @Test
    public void testParseError() throws JsInvoker.SyntaxException {
        //error
        JsInvoker.ParserResult pr = new JsInvoker.Parser("obj.call(abc,, def, 123)").parse();
        System.out.println(pr.toString());

        //error
        pr = new JsInvoker.Parser("obj.call(abc), def, 123)").parse();
        System.out.println(pr.toString());

        //error
        pr = new JsInvoker.Parser("obj.call(abc, def, 123),").parse();
        System.out.println(pr.toString());
    }
}
