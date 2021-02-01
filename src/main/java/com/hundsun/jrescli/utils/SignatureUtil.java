package com.hundsun.jrescli.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
/*
 * 系统名称: 云支撑平台
 * 模块名称: 应用管理模块
 * 文件名称: RestSignatureUtil.java
 * 软件版权: 恒生电子股份有限公司
 * 修改记录:
 * 修改日期            修改人员                     修改说明 <br>
 * ========    =======  ============================================
 *
 * ========    =======  ============================================
 */

/**
 * REST请求签名以及签名校验工具类, 负责HTTP请求的签名计算以及签名校验
 *
 * @author zhujy
 * @version 1.0
 */
public class SignatureUtil {

	private static final String API_VERSION = "v1";

	private static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	private static final String ENCODING = "UTF-8";

	private static final String RESPONSE_FORMAT = "JSON";

	private static final int REQUEST_TIMEOUT = 120;// 请求超时时间（秒）

	/**
	 * 判定指定的请求是否超时以及是否未被篡改
	 *
	 * @param httpMethod      HTTP请求方法
	 * @param accessKeySecret 请求加密KEY
	 * @param parameters      参数列表
	 * @return 是否未被篡改
	 * @throws SignatureException
	 */
	public static void verify(String httpMethod, String accessKeySecret, Map<String, String> parameters)
			throws SignatureException {
		String timeStamp = parameters.get("TimeStamp");
		if (timeStamp == null) {
			throw new SignatureException(400, "timestamp.empty");
		}
		Date timestamp = null;
		try {
			timestamp = parseTimestamp(timeStamp);
		} catch (Exception e) {
			throw new SignatureException(400, "timestamp.illegal");
		}
		if (System.currentTimeMillis() - timestamp.getTime() > TimeUnit.SECONDS.toMillis(REQUEST_TIMEOUT)) {
			throw new SignatureException(408, "timestamp.timeout");
		}
		String requestSignature = parameters.remove("Signature");
		String signatureMethod = parameters.get("SignatureMethod");
		String newSignature = calculateSignature(httpMethod, accessKeySecret,
				signatureMethod.equals("HMAC-SHA1") ? "HmacSHA1" : signatureMethod, parameters);
		if (!newSignature.equals(requestSignature)) {
			throw new SignatureException(400, "signature.disaccord");
		}
	}

	/*
	 * 解析请求时间戳, 用于判定是否超时
	 */
	private static Date parseTimestamp(String timeStamp) throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATE_FORMAT);
		// 注意使用GMT时间
		df.setTimeZone(new SimpleTimeZone(0, "GMT"));
		return df.parse(timeStamp);
	}

	/**
	 * 为参数计算签名
	 *
	 * @param accessKeyId     登入用户ID
	 * @param accessKeySecret 登录密钥
	 * @param action          请求方法名（用户使用同样的请求地址, 通过参数指定请求的情况）
	 * @param httpMethod      http方法
	 * @param parameters      参数列表
	 * @return 加入签名后的参数列表
	 * @throws SignatureException
	 */
	public static final Map<String, String> sign(String accessKeyId, String accessKeySecret, String action,
			String httpMethod, Map<String, String> parameters) throws SignatureException {
		if (action == null)
			throw new SignatureException(400, "signature.ActionIsEmpty");
		parameters.put("Action", action);
		parameters.put("Version", API_VERSION);
		parameters.put("AccessKeyId", accessKeyId);
		parameters.put("TimeStamp", formatIso8601Date(new Date()));
		parameters.put("SignatureMethod", "HMAC-SHA1");
		parameters.put("SignatureVersion", "1.0");
		parameters.put("SignatureNonce", UUID.randomUUID().toString()); // 可以使用UUID作为SignatureNonce
		parameters.put("Format", RESPONSE_FORMAT);
		// 计算签名, 并将签名结果加入请求参数中
		try {
			parameters.put("Signature",
					calculateSignature(httpMethod, getSHA512(accessKeySecret), "HmacSHA1", parameters));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return parameters;
	}

	private static String toHex(byte[] digest) {
		StringBuilder sb = new StringBuilder();
		int len = digest.length;
		String out = null;
		for (int i = 0; i < len; i++) {
			out = Integer.toHexString(0xFF & digest[i]);// 原始方法
			if (out.length() == 1) {
				sb.append("0");// 如果为1位 前面补个0
			}
			sb.append(out);
		}
		return sb.toString();
	}

	/*
	 * 格式化时间戳
	 */
	private static String formatIso8601Date(Date date) {
		SimpleDateFormat df = new SimpleDateFormat(ISO8601_DATE_FORMAT);
		// 注意使用GMT时间
		df.setTimeZone(new SimpleTimeZone(0, "GMT"));
		return df.format(date);
	}

	/*
	 * 计算参数列表签名数据
	 */
	private static final String calculateSignature(String httpMethod, String accessKeySecret, String ALGORITHM,
			Map<String, String> parameters) throws SignatureException {
		// 将参数Key按字典顺序排序
		String[] sortedKeys = parameters.keySet().toArray(new String[] {});
		Arrays.sort(sortedKeys);

		final String SEPARATOR = "&";
		// 生成规范化请求字符串
		StringBuilder canonicalizedQueryString = new StringBuilder();
		for (String key : sortedKeys) {
			canonicalizedQueryString.append("&").append(percentEncode(key)).append("=")
					.append(percentEncode(parameters.get(key)));
		}

		// 生成用于计算签名的字符串 stringToSign
		StringBuilder stringToSign = new StringBuilder();
		stringToSign.append(httpMethod).append(SEPARATOR);
		stringToSign.append(percentEncode("/")).append(SEPARATOR);
		stringToSign.append(percentEncode(canonicalizedQueryString.toString().substring(1)));
		// 注意accessKeySecret后面要加入一个字符"&"
		String signature = calculate(accessKeySecret + "&", ALGORITHM, stringToSign.toString());
		return signature;
	}

	public static String getMd5Password(String password) {

		String passwordMd5 = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(password.getBytes());
			// 进行哈希计算并返回结果
			byte[] btResult = messageDigest.digest();
			StringBuffer sb = new StringBuffer();

			for (byte b : btResult) {
				int bt = b & 0xff;
				if (bt < 16) {
					sb.append(0);
				}
				sb.append(Integer.toHexString(bt));
			}
			passwordMd5 = sb.toString();

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return passwordMd5;
	}

	/**
	 * 获取指定字符串的SHA-512码
	 *
	 * @param input 输入数据
	 * @return sha-512加密数据
	 * @throws Exception
	 */
	public static String getSHA512(String input) throws Exception {
		MessageDigest digiestor = MessageDigest.getInstance("SHA-512");
		digiestor.update(input.getBytes("utf-8")); // 先更新摘要
		byte[] digest = digiestor.digest();
		return toHex(digest);
	}

	/*
	 * 计算签名
	 */
	private static String calculate(String key, String ALGORITHM, String stringToSign) throws SignatureException {
		// 使用HmacSHA1算法计算HMAC值
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(key.getBytes(ENCODING), ALGORITHM));
			byte[] signData = mac.doFinal(stringToSign.getBytes(ENCODING));
			return new String(Base64.encodeBase64(signData));
		} catch (Exception e) {
			throw new SignatureException(400, "signature.cipherFail");
		}
	}

	/*
	 * 格式化参数非法字符
	 */
	private static String percentEncode(String value) throws SignatureException {
		// 使用URLEncoder.encode编码后, 将"+","*","%7E"做替换即满足API规定的编码规范
		try {
			return value != null
					? URLEncoder.encode(value, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~")
					: null;
		} catch (UnsupportedEncodingException e) {
			throw new SignatureException(400, "signature.unsupportedEncoding");
		}
	}

	/**
	 * 签名计算与验证异常类, 提供异常消息与Http Code
	 *
	 * @author zhujy
	 * @version 1.0
	 */
	public static final class SignatureException extends Exception {
		private static final long serialVersionUID = -340723756188549853L;
		private int httpStatus;

		SignatureException(int httpStatus, String message) {
			super(message);
			this.httpStatus = httpStatus;
		}

		public int getHttpCode() {
			return httpStatus;
		}

	}
}