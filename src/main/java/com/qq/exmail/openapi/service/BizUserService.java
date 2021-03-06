package com.qq.exmail.openapi.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jodd.json.JsonParser;
import jodd.util.StringUtil;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.qq.exmail.openapi.BaseService;
import com.qq.exmail.openapi.BizMail;
import com.qq.exmail.openapi.BizMailException;
import com.qq.exmail.openapi.OpenApiConst;
import com.qq.exmail.openapi.model.BizUser;
import com.qq.exmail.openapi.model.jodd.Item;
import com.qq.exmail.openapi.model.jodd.Result;
import com.qq.exmail.openapi.utils.Md5Utils;


/**
 * 同步成员帐号资料
 * 
 * 例： action=3&alias=bob@gzdev.com&name=BOB&gender=1&position=engineer&slave=test@gzdev.com
 * Action string 1=DEL, 2=ADD, 3=MOD
 * Alias string 帐号名，帐号名为邮箱格式
 * Name string 姓名
 * Gender string 性别：0=未设置，1=男，2=女
 * Position string 职位
 * Tel string 联系电话
 * Mobile string 手机
 * ExtId string 编号
 * Password string 密码
 * Md5 string 是否为 Md5 密码，0=明文密码，1=Md5 密码，
 * PartyPath string 所属部门
 * 1、传部门路径，用’/’分隔
 * 2、根部门不需要传。如果空，则为根部门。
 * 部门是已存在的
 * 3、如果是多个部门，传多个 PartyPath
 * Slave string 别名列表
 * 1. 如果多个别名，传多个 Slave
 * 2. Slave 上限为 5 个
 * 3. Slave 为邮箱格式
 * OpenType string 0=不设置状态，1=启用帐号，2=禁用帐号
 * 
 * @author 张宗荣
 * 
 */
public class BizUserService extends BaseService {
	/**
	 * 日志
	 */
	private static Logger logger = LogManager.getLogger(BizUserService.class);
	/**
	 * JSON反序列化
	 */
	private	 JsonParser JSONPARSER = new JsonParser();
	
	/**
	 * 帐号名无效
	 */
	final Integer AliasState_Err = -1;
	/**
	 * 帐号名未被占用
	 */
	final Integer AliasState_OK = 0;
	/**
	 * 主帐号
	 */
	final Integer AliasState_Account = 1;
	/**
	 * 别名
	 */
	final Integer AliasState_Alias = 2;
	/**
	 * 邮件群组帐号
	 */
	final Integer AliasState_Group = 3;
	
	/**
	 * 获取成员资料
	 * 
	 * @param alias 主帐号 或者 别名
	 * @return null表示帐号不存在
	 * @throws BizMailException
	 */
	public BizUser query(String alias) throws BizMailException {
		Map<String, String> queryParam = new LinkedHashMap<String, String>(2);
		queryParam.put("alias", alias);
		try {
			String responseTxt = ApiGet(OpenApiConst.USER_GET_URL, queryParam);
			logger.debug(responseTxt);
			BizUser bizUser = JSONPARSER.parse(responseTxt, BizUser.class);
			return bizUser;
		}catch (BizMailException bme){
			// BizMail接口出错[1301]user_not_found
			if ("1301".equals(bme.getErrcode())) {
				return null;
			} else {
				throw bme;
			}
		}
	}
	
	final String RESP_JSON_AUTHKEY="auth_key";//AuthKey
	final String RESP_JSON_NEWCOUNT = "NewCount";
	
	/**
	 * 获取Authkey
	 *
	 * @param alias
	 * @return String
	 * @throws BizMailException
	 */
	public String authkey(String alias) throws BizMailException{
		Map<String, String> queryParam = new LinkedHashMap<String, String>(2);
		queryParam.put("alias", alias);
		String responseTxt = ApiGet(OpenApiConst.AUTH_URL, queryParam);
		logger.debug(responseTxt);
		JsonParser jsonParser = new JsonParser();
		Map map = jsonParser.parse(responseTxt, Map.class);
		String authkey = map != null && null != map.get(RESP_JSON_AUTHKEY) ? map.get(RESP_JSON_AUTHKEY).toString() : null;
		return authkey;
	}
	
	/**
	 * 用户一键登录地址
	 * @param alias
	 * @return
	 * @throws BizMailException
	 */
	public String ssologin(String alias) throws BizMailException {
		return ssologin(alias, null);
	}
	
	/**
	 * 用户一键登录地址，指定自定义域名（不建议）
	 *
	 * @param alias
	 * @param domailUrl
	 * @return
	 * @throws BizMailException
	 */
	public String ssologin(String alias,String domailUrl) throws BizMailException {
		StringBuilder ssoUrl = new StringBuilder(256);
		if (StringUtil.isBlank(domailUrl)) {
			ssoUrl.append(OpenApiConst.LOGIN_URL);
		} else if (!domailUrl.startsWith("http://") && domailUrl.startsWith("https://")) {
			ssoUrl.append("http://").append(domailUrl);
		} else {
			ssoUrl.append(domailUrl);
		}
		ssoUrl.append("?").append("fun=bizopenssologin").append("&method=bizauth").append("&agent=").append(BizMail.getClientId()).append("&user=").append(alias).append("&ticket=");
		String ticket = authkey(alias);
		if (StringUtil.isBlank(ticket)) {
			throw new BizMailException("获取单点登录认证信息出错");
		}
		ssoUrl.append(ticket);
		return ssoUrl.toString();
	}
	
	
	/**
	 * 返回帐号未读邮件数，当帐号不存在时将返回 -1
	 *
	 * @param alias
	 * @return Integer
	 * @throws BizMailException
	 */
	public Integer newcount(String alias) throws BizMailException {
		Integer count = 0;
		Map<String, String> queryParam = new LinkedHashMap<String, String>(2);
		queryParam.put("alias", alias);
		try {
			String responseTxt = ApiGet(OpenApiConst.MAIL_NEWCOUNT_URL, queryParam);
			logger.debug(responseTxt);
			// 返回示例 {"Alias":"zzr@vip2.msdi.cn","NewCount":"2"}
			JsonParser jsonParser = new JsonParser();
			Map map = jsonParser.parse(responseTxt, Map.class);
			if (map != null && null != map.get(RESP_JSON_NEWCOUNT)) {
				String newcount = map.get(RESP_JSON_NEWCOUNT).toString();
				count = Integer.parseInt(newcount);
			}
		} catch (BizMailException bme) {
			if ("1400".equals(bme.getErrcode())) {
				count = -1;
			} else {
				throw bme;
			}
		}
		return count;
	}

	/**
	 * 新增员工邮箱
	 *
	 * @param bizUser
	 * @return
	 * @throws BizMailException
	 */
	public boolean add(BizUser bizUser) throws BizMailException {
		//如果密码为空则直接异常
		if(StringUtil.isBlank(bizUser.getPassword()) ){
			throw new BizMailException("新增邮箱帐号时密码不能为空");
		}
		bizUser.setAction(OpenApiConst.OP_ADD);
		bizUser.setOpenType(OpenApiConst.ENABLE_USER);

		String bizUserParam = bizUser.serialize();
		logger.debug(bizUserParam);
		if (bizUser.getSlave() == null && bizUser.getPartyPath() == null) {
			Map<String, Object> formData = bizUser.toPostForm();//20170327 使用Map仅能创建一个别名的帐号
			// 处理密码为MD5
			//boolean r = fixPassword(formData);
			String responseTxt = ApiPost(OpenApiConst.USER_SYNC_URL, formData);//添加成功无返回
		} else {
			// 实现添加多个邮箱别名
			String responseTxt = ApiGet(OpenApiConst.USER_SYNC_URL, bizUserParam);
		}
;
		return true;
	}


	/**
	 * 更新帐号信息
	 *
	 * @param bizUser
	 * @return
	 * @throws BizMailException
     */
	public boolean modify(BizUser bizUser) throws BizMailException {
		bizUser.setAction(OpenApiConst.OP_MOD);
		//bizUser.setOpenType(OpenApiConst.ENABLE_USER);
		logger.debug("修改成员帐号：" + bizUser.serialize());
		if (bizUser.getSlave() == null && bizUser.getPartyPath() == null) {
			Map<String, Object> formData = bizUser.toPostForm();
			// 处理密码为MD5
			//boolean r = fixPassword(formData);
			String responseTxt = ApiPost(OpenApiConst.USER_SYNC_URL, formData);//修改操作无返回
		} else {
			String responseTxt = ApiGet(OpenApiConst.USER_SYNC_URL, bizUser.serialize()); //fix 使用Get请求解决添加多个邮箱别名的问题
		}
		return true;
	}


	/**
	 * 禁用用户帐号
	 * <b>可恢复</b>
	 *
	 * @param bizUser
	 * @return
	 * @throws BizMailException
	 */
	public boolean disable(BizUser bizUser) throws BizMailException {
		bizUser.setAction(OpenApiConst.OP_MOD);
		bizUser.setOpenType(OpenApiConst.DISABLE_USER);
		Map<String, Object> formData = bizUser.toPostForm();
		String responseTxt = ApiPost(OpenApiConst.USER_SYNC_URL, formData);//禁用操作无返回文本
		logger.debug(responseTxt);
		return true;
	}

	/**
	 * 删除用户帐号
	 * <b>直接删除，无法恢复</b>
	 *
	 * @param bizUser
	 * @return
	 * @throws BizMailException
	 */
	public boolean delete(BizUser bizUser) throws BizMailException {
		bizUser.setAction(OpenApiConst.OP_DEL);
		Map<String, Object> formData = bizUser.toPostForm();
		String responseTxt = ApiPost(OpenApiConst.USER_SYNC_URL, formData);//删除操作无返回
		logger.debug(responseTxt);
		return true;
	}
	
	/**
	 * 安全删除用户的方法，如果用户有未读邮件则不执行删除
	 *
	 * @param bizUser
	 * @return
	 * @throws BizMailException
	 */
	@Deprecated
	public boolean deleteSafe(BizUser bizUser) throws BizMailException{
		//TODO 尚未实现
		//1、判断用户是否有未读邮件
		//2、如无则执行删除操作
		return false;
	}

	// public BizUser check(Object... emails) throws BizMailException {
	// if (null != emails && emails.length > 0) {
	// for (Object e : emails) {
	// if (e != null && StringUtil.isNotEmpty(e.toString()))
	// queryParam.put("email", e.toString());
	// }
	// }

	
	/**
	 * 检查邮件帐号是否可用
	 * 邮件帐号(如果多个需要检查的邮件帐号，传多个email，email 上限为20 个)
	 *
	 * @param alias
	 * @return
	 * @throws BizMailException
	 */
	public boolean isValidate(String alias) throws BizMailException {
		Map<String, String> queryParam = new LinkedHashMap<String, String>();
		queryParam.put("email", alias);
		String responseTxt = ApiGet(OpenApiConst.USER_CHECK_URL, queryParam);
		logger.debug(responseTxt);
		// 返回示例： 
		// {"Count": 1,"List": [{"Email": "02166@vip2.msdi.cn","Type": 2}]}
		// {"Count": 1,"List": [{"Email": "02166@vip2.msdi.cn","Type": 0}]}
		// {"Count": 0,"List": []}
		//解决复杂对象JSON解析问题
		Result result = JSONPARSER.parse(responseTxt, Result.class);
		if (null != result && result.getCount() > 0) {
			Item list = result.getList().get(0);
			if (AliasState_OK.equals(list.getType()))
				return true;
		}
		
		return false;			
	}
	
	/**
	 * 获取系统中所有帐号的清单
	 * 	应用于人员信息同步或数据初始化
	 *
	 * @return List
	 * @throws BizMailException
	 */
	public List<String> queryAllUsers() throws BizMailException{
		return userlist(0, null);
	}
	
	/**
	 * 获取系统指定Ver后的变更用户List
	 *
	 * @param ver
	 * @param actionType
	 * @return
	 * @throws BizMailException
	 */
	public List<String> userlist(Integer ver,Integer actionType) throws BizMailException {
		Map<String, String> queryParam = new LinkedHashMap<String, String>();
		queryParam.put("Ver", ver.toString());
		String responseTxt = ApiGet(OpenApiConst.USER_LIST_URL, queryParam);
		//logger.debug(responseTxt);//内容可能过长，不记录日志
		// 返回示例： 
		// {"Ver": 1474444854395,"Count": 1,"List": [{"Alias": "02166@vip2.msdi.cn","Action": 2}]}
		// {"Ver": 1474444854300,"Count": 0,"List": []}
		List<String> users = new ArrayList<String>(128);
		Result result = JSONPARSER.parse(responseTxt, Result.class);
		if (null != result && result.getCount() > 0) {
			if (null == actionType) {
				for (Item item : result.getList()) {
					users.add(item.getAlias());
				}
			} else {
				for (Item item : result.getList()) {
					if (actionType.equals(item.getAction()))
						users.add(item.getAlias());
				}
			}
		}
		return users;
	}
	
	
	// ---------------------------------------------------------------- 私有方法

	/**
	 * 处理用户对象中的密码属性
	 * 明文密码处理为MD5
	 * 
	 * @param formData
	 * @return true带有密码，false 无密码
	 */
	private boolean fixPassword(Map<String, Object> formData) {
		Object pwdObj = formData.get("Password");
		if (pwdObj == null || StringUtil.isBlank(pwdObj.toString())) {
			formData.remove("Password");
			formData.remove("Md5");
			return false;
		}
		//密码加密移至 setPassword中实现
//		String pwd = pwdObj.toString();
//		if (pwd.length() < 6) {
//			// 密码长度不够
//		}
//		// 支持修改密码
//		String md5Pwd = Md5Utils.md5(pwdObj.toString());
//		formData.put("Md5", "1");
//		formData.put("Password", md5Pwd);
		return true;
	}

}
