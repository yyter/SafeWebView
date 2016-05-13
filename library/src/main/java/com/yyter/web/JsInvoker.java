package com.yyter.web;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by liyang on 5/12/16.
 */
class JsInvoker {
    private final HashMap<String, Object> javaBridgeMap = new HashMap<>();
    public void registerJavaBridge(Object javaBridge, String identifier) {
        if(javaBridge == null) {
            throw new IllegalArgumentException("javaBridge can't be null!");
        }
        if(!isValidJavaIdentifier(identifier)) {
            throw new IllegalArgumentException("not a valid java identifier");
        }

        javaBridgeMap.put(identifier, javaBridge);
    }

    public Object invoke(String statement) throws SyntaxException {
        ParserResult parserResult = new Parser(statement).parse();
        if(parserResult != null) {
            Object javaBridge = javaBridgeMap.get(parserResult.identifier);
            if(javaBridge != null) {
                try {
                    Method method = javaBridge.getClass().getMethod(parserResult.methodName, getParamsTypes(parserResult.params));

                    if(method.getAnnotation(JavascriptInvoker.class) != null) {
                        method.setAccessible(true);
                        return method.invoke(javaBridge, parserResult.params);
                    } else {
                        Log.e("SafeWebView", "method should have @JavascriptInvoker annotation if you want to call through javascript");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static Class<?>[] getParamsTypes(String[] params) {
        if(params == null || params.length == 0) {
            return null;
        }

        Class<?>[] clazzes = new Class[params.length];
        for(int i = 0, count = params.length; i < count; ++i) {
            clazzes[i] = String.class;
        }
        return clazzes;
    }

    /**
     * 验证 字符串是否是合法的java标识符
     */
    private static boolean isValidJavaIdentifier(String identifier) {
        if(identifier == null) {
            return false;
        }
        if(!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return false;
        }

        for(int i = 1, count = identifier.length(); i < count; ++i) {
            if(!Character.isJavaIdentifierPart(identifier.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** package for junit test */ static class ParserResult {
        final String identifier;
        final String methodName;
        final String[] params;

        public ParserResult(String identifier, String methodName, String[] params) {
            this.identifier = identifier;
            this.methodName = methodName;
            this.params = params;
        }

        @Override
        public String toString() {
            return "ParserResult{" +
                    "identifier='" + identifier + '\'' +
                    ", methodName='" + methodName + '\'' +
                    ", params=" + Arrays.toString(params) +
                    '}';
        }
    }

    /** package for junit test */ static class SyntaxException extends Exception {
        public SyntaxException(String detailMessage) {
            super(detailMessage);
        }
    }

    /** package for junit test */ static class Parser {
        private int index;
        private StringBuilder buf = new StringBuilder();
        private String statement;
        Parser(String statement) {
            this.statement = statement;
        }

        ParserResult parse() throws SyntaxException {
            if(statement == null) {
                return null;
            }

            String identifier = readIdentifier('.');
            String methodName = readIdentifier('(');
            String[] params = readParams();

            return new ParserResult(identifier, methodName, params);
        }

        private String readIdentifier(char endToken) throws SyntaxException {
            clearBuffer();
            int c;
            while ((c = nextToken()) != -1) {
                if(c == endToken) {
                    if(isBufferEmpty()) {
                        throw new SyntaxException("identifier is empty. statement: " + statement);
                    }
                    return buf.toString();
                } else {
                    if(isBufferEmpty() && !Character.isJavaIdentifierStart(c)) {
                        //first char
                        throw new SyntaxException("not a valid java identifier, invalid first character: " + c);
                    } else if(!Character.isJavaIdentifierPart(c)) {
                        throw new SyntaxException("not a valid java identifier, invalid character: " + c);
                    }
                    buf.append((char) c);
                }
            }

            if(isBufferEmpty()) {
                throw new SyntaxException("empty identifier");
            }
            return buf.toString();
        }

        private static final int NONE_TYPE = -1;
        private static final int WORD_TYPE = 0;
        private static final int END_TYPE = 1;
        private static final int ESCAPE_TYPE = 2;
        private static final int COMA_TYPE = 3;
        private static final HashMap<Character, Character> escapeChar = new HashMap<>(5);
        static {
            escapeChar.put('r', '\r');
            escapeChar.put('n', '\n');
            escapeChar.put('b', '\b');
            escapeChar.put('t', '\t');
            escapeChar.put('f', '\f');
        }

        private String[] readParams() throws SyntaxException {
            clearBuffer();
            ArrayList<String> params = new ArrayList<>();
            int lastTokenType = NONE_TYPE;
            int c;
            while ((c = nextToken()) != -1) {
                if(lastTokenType == END_TYPE) {
                    if(c == ';' || Character.isWhitespace(c)) {
                        continue;
                    } else {
                        throw new SyntaxException("invalid character after statement has end");
                    }
                }

                switch (c) {
                    case ',':
                        if(lastTokenType == ESCAPE_TYPE) {
                            buf.append(',');
                            lastTokenType = WORD_TYPE;
                        } else if(lastTokenType == WORD_TYPE) {
                            params.add(buf.toString());
                            clearBuffer();
                            lastTokenType = COMA_TYPE;
                        } else {
                            throw new SyntaxException("invalid coma in params list");
                        }
                        break;
                    case ')':
                        if(lastTokenType == NONE_TYPE) {
                            //empty param list
                            lastTokenType = END_TYPE;
                        } else if(lastTokenType == ESCAPE_TYPE) {
                            buf.append(')');
                            lastTokenType = WORD_TYPE;
                        } else if(lastTokenType == WORD_TYPE) {
                            params.add(buf.toString());
                            clearBuffer();
                            lastTokenType = END_TYPE;
                        } else {
                            throw new SyntaxException("invalid character after ')'");
                        }
                        break;
                    case '\\':
                        if(lastTokenType == ESCAPE_TYPE) { //2个 \\ 识别为 \
                            buf.append('\\');
                            lastTokenType = WORD_TYPE;
                        } else {
                            lastTokenType = ESCAPE_TYPE;
                        }
                        break;
                    case 'r':
                    case 'n':
                    case 'f':
                    case 't':
                    case 'b':
                        if(lastTokenType == ESCAPE_TYPE) {
                            buf.append(escapeChar.get((char) c));
                        } else {
                            buf.append((char) c);
                        }
                        lastTokenType = WORD_TYPE;
                        break;
                    default:
                        buf.append((char) c);
                        lastTokenType = WORD_TYPE;
                        break;
                }
            }

            if(lastTokenType != END_TYPE) {
                throw new SyntaxException("params list syntax error");
            }

            return params.isEmpty() ? null : params.toArray(new String[params.size()]);
        }

        private void clearBuffer() {
            buf.delete(0, buf.length());
        }

        private boolean isBufferEmpty() {
            return buf.length() == 0;
        }

        /**
         * @return 下一个字符,如果到了结尾,返回-1
         */
        private int nextToken() {
            if(index >= statement.length()) {
                return -1;// the end
            }
            return statement.charAt(index++);
        }
    }
}
