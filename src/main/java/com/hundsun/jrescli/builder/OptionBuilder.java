package com.hundsun.jrescli.builder;

import org.apache.commons.cli.Options;

public class OptionBuilder {

	public Options argsOption() {
		Options opts = new Options();
		// see 地址
		opts.addOption("h", true, "see address ip");
		// see 用户名
		opts.addOption("u", true, "see username");
		// see 密码
		opts.addOption("p", true, "see user password");
		// 接口名称
		opts.addOption("a", true, "request action");
		// http请求类型GET或POST
		opts.addOption("t", true, "resource type");
		// 资源库ip
		// opts.addOption("i", true, resourceBundle.getString("resource ip"));
		// 资源库路径
		// opts.addOption("r", true, resourceBundle.getString("resource address"));
		// 环境id 默认是001
		opts.addOption("e", true, "environment id");
		// 文件路径路径
		opts.addOption("f", true, "file path");
		return opts;
	}
}
