package org.example.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class ExprCalculate {
    private int index = 0;

    public int calculate(String s) {
        index = 0;
        return helper(s.replaceAll("\\s+", "")); // 去掉所有空白
    }

    private int helper(String s) {
        Deque<Integer> stack = new ArrayDeque<>();
        char operator = '+';
        int num = 0;

        while (index < s.length()) {
            char c = s.charAt(index++);

            if (!Character.isDigit(c) && "+-*/()".indexOf(c) == -1) {
                throw new IllegalArgumentException("Invalid character: " + c);
            }

            if (Character.isDigit(c)) {
                num = num * 10 + (c - '0');
            }

            if (c == '(') {
                num = helper(s); //求子表达式
            }

            // 到达末尾或读到运算符/右括号时，根据“之前的 operator”结算当前 num
            if (index == s.length() || "+-*/)".indexOf(c) != -1) {
                switch (operator) {
                    case '+': stack.push(num); break;
                    case '-': stack.push(-num); break;
                    case '*': stack.push(stack.pop() * num); break;
                    case '/':
                        stack.push(stack.pop() / num);
                        break;
                }
                num = 0;

                // 右括号，结束本层，
                if (c == ')') break;

                // +-*/更新 operator
                if ("+-*/".indexOf(c) != -1) {
                    operator = c;
                }
            }
        }

        int result = 0;
        while (!stack.isEmpty()) result += stack.pop();
        return result;
    }
}
