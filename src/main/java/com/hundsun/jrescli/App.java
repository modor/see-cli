package com.hundsun.jrescli;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

		Map<String, String> applicationListMaps = new HashMap<String, String>() {
			{
				put("Action", "ApplicationList");
				put("ProductType", "SEE.PRODUCT");
				put("EnvironmentId", "001");
			}
		};
		String applicationListUrl = "http://" + cmd.getOptionValue("h") + "/acm/api/v1/application?"
				+ getParamsString(SignatureUtil.sign(cmd.getOptionValue("u"), cmd.getOptionValue("p"),
						"ApplicationList", "GET", applicationListMaps));
		// System.out.println(ApplicationListUrl);
		List<Map<String, String>> appList = new ObjectMapper().readValue(getReponse(applicationListUrl, "GET"),
				ArrayList.class);
		for (Map<String, String> app : appList) {
			Map<String, String> appDeployByIdMap = new HashMap<String, String>() {
				{
					put("Action", "ApplicationDeployById");
					put("EnvironmentId", "001");
				}
			};
			appDeployByIdMap.put("SystemId", app.get("id"));
			String applicationDeployByIdUrl = "http://" + cmd.getOptionValue("h") + "/acm/api/v1/application?"
					+ getParamsString(SignatureUtil.sign(cmd.getOptionValue("u"), cmd.getOptionValue("p"),
							"ApplicationDeployById", "POST", appDeployByIdMap));
			String deployResult = getReponse(applicationDeployByIdUrl, "POST");
			if (deployResult == null || deployResult.equals("")) {
				System.out.println("Failed Application:" + app);
			}
			Thread.sleep(1000);
		}

	}
}
