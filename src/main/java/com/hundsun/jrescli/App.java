package com.hundsun.jrescli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hundsun.jrescli.builder.OptionBuilder;
import com.hundsun.jrescli.utils.SignatureUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class App {

	private static String getParamsString(Map<String, String> params)
			throws SignatureUtil.SignatureException, UnsupportedEncodingException {
		StringBuilder stringBuilder = new StringBuilder();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			stringBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=")
					.append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
		}

		String str = stringBuilder.toString();
		return str.substring(0, str.length() - 1);
	}

	private static String getReponse(String httpUrl, String requestType) throws IOException {
		URL url = new URL(httpUrl);
		StringBuffer result = new StringBuffer();
		BufferedReader br = null;
		InputStream is = null;
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// 设置请求方式
		connection.setRequestMethod(requestType);
		// 设置连接超时时间
		connection.setReadTimeout(3600000);
		// 开始连接
		connection.connect();
		// 获取响应数据
		if (connection.getResponseCode() == 200) {
			// 获取返回的数据
			is = connection.getInputStream();
			if (null != is) {
				br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				String temp = null;
				while (null != (temp = br.readLine())) {
					result.append(temp);
				}
			}
		}
		return result.toString();
	}

	public static void main(String[] args) throws Exception {
		Options options = new OptionBuilder().argsOption();
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		File jsonFile = new File(cmd.getOptionValue("f"));
		Map<String, String> maps = null;
		if (jsonFile.isFile() && jsonFile.exists()) {
			ObjectMapper mapper = new ObjectMapper();
			maps = mapper.readValue(FileUtils.readFileToString(jsonFile, "UTF-8"), Map.class);
		}

		String url = "http://" + cmd.getOptionValue("h") + "/acm/api/v1/application?"
				+ getParamsString(SignatureUtil.sign(cmd.getOptionValue("u"), cmd.getOptionValue("p"),
						cmd.getOptionValue("a"), cmd.getOptionValue("t"), maps));

		System.out.println(url);
		System.out.println(getReponse(url, cmd.getOptionValue("t")));
	}
}
