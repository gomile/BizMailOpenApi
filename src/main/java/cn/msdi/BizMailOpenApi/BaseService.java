package cn.msdi.BizMailOpenApi;

import java.util.LinkedHashMap;
import java.util.Map;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import jodd.json.JsonParser;
import cn.msdi.BizMailOpenApi.model.BizError;
import cn.msdi.BizMailOpenApi.oauth.OAuth2;

public class BaseService {
	/*
	 * 调用的方式有两种方式：
	 * POST 方式：在POST 请求加上access_token；
	 * GET 或者其他方式： 在HTTP HEAD 加上Authorization，将client_id 和client_secret 以
	 * BASE64 加密方式加密，即base64(client_id: client_secret)，将密文发送到请求信息中。
	 */

	// TODO 请求接口过程中由于网络或者超时原因，token票据失效或请求失败导致
	// 建议同一数据请求失败一次后再次请求，降低错误率
	// 1002 temporarily_unavailable 暂时不可用
	// 1200	invalid_token token值无效

	
	/**
	 * 
	 * @return
	 * @throws BizMailException
	 */
	protected String ApiPost(String end, Map<String, Object> formData) throws BizMailException {
		String body = null;
		HttpRequest request = HttpRequest.post(end).formEncoding("UTF-8");
		request.form(formData);
		//formData.put("access_token", OAuth2.getInstance().getToken().getAccess_token());
		String token = "Bearer" + " " + OAuth2.getInstance().getToken().getAccess_token();
		request.header("Authorization", token, true);
		HttpResponse response = request.send();
		if (response.statusCode() == 200) {
			body = response.bodyText();
			if (body.indexOf("errcode") > 0) {
				JsonParser jsonParser = new JsonParser();
				BizError bizError = jsonParser.parse(body, BizError.class);
				throw new BizMailException("BizMail接口出错", bizError);
			}
		} else {
			throw new BizMailException("BizMail接口Api请求失败");
		}
		return body;
	}

	/**
	 * 
	 * @return
	 * @throws BizMailException
	 */
	protected String ApiGet(String end, Map<String, String> queryMap) throws BizMailException {
		String body = null;
		if (null == queryMap) {
			queryMap = new LinkedHashMap<String, String>(1);
		}
		queryMap.put("access_token", OAuth2.getInstance().getToken().getAccess_token());
		
		HttpRequest request = HttpRequest.get(end);
		request.queryEncoding("UTF-8").query(queryMap);
		HttpResponse response = request.send();
		if (response.statusCode() == 200) {
			body = response.bodyText();
			if (body.indexOf("errcode") > 0) {
				JsonParser jsonParser = new JsonParser();
				BizError bizError = jsonParser.parse(body, BizError.class);
				throw new BizMailException("BizMail接口出错", bizError);
			}
		} else {
			throw new BizMailException("BizMail接口Api请求失败");
		}
		return body;
	}
}
