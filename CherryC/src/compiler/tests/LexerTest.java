package compiler.tests;

import compiler.Lexer;
import compiler.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by david on 4/12/17.
 */
class LexerTest
{
	void testForTokens(String testString, String[] tokens)
	{
		InputStream inputStream = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);

		Lexer lexer = new Lexer(pushbackInputStream, "Test.cherry");

		for (String token : tokens)
		{
			Token tok = lexer.getToken();
			Assertions.assertEquals(token, tok.value);
		}

		// Make sure all test cases end with an EOF token. //
		Assertions.assertEquals(Token.TokenType.EOF, lexer.getToken().tokenType);

	}


	@Test
	void getToken()
	{

		testForTokens("var a = 7123", new String[]{"var", "a", "=", "7123"});
		testForTokens("func b():", new String[]{"func", "b", "(", ")", ":"});
		testForTokens("\tvar a = 7123", new String[]{"", "var", "a", "=", "7123"});
		testForTokens("func b(a : int, b : int) -> int:", new String[]{
				"func", "b", "(", "a", ":", "int", ",", "b", ":", "int", ")", "->", "int", ":"});
		System.out.println("Test/s successful.");




	}

}