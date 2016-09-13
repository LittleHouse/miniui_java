package com.lifang.userapp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.leo.common.util.DataUtil;
import com.lifang.agentcommsoa.model.ResponseDistrictAndTown;
import com.lifang.agentcommsoa.model.Town;
import com.lifang.agentcommsoa.service.AgentCommonService;
import com.lifang.agentcommsoa.util.ResponseStatus;
import com.lifang.agentsoa.facade.AgentSOAServer;
import com.lifang.agentsoa.model.Agent;
import com.lifang.agentsoa.model.AgentComment;
import com.lifang.agentsoa.model.DicAreaNew;
import com.lifang.bidsoa.model.HttpRequestHouseBidModel;
import com.lifang.bidsoa.model.HttpRequestHouseBidOperateModel;
import com.lifang.bidsoa.model.HttpResponseHouseBidRecordDetailInfoModel;
import com.lifang.bidsoa.model.HttpResponseHouseBidRuleModel;
import com.lifang.bidsoa.rmi.RMIHouseBidService;
import com.lifang.countsoa.model.HttpRequestChannelCounterModel;
import com.lifang.countsoa.rmi.RMIChannelCounterService;
import com.lifang.customeragentsoa.facade.CustomerAgentSOAServer;
import com.lifang.imgsoa.facade.ImageService;
import com.lifang.imgsoa.model.ImageKeyObject;
import com.lifang.imsoa.facade.ImUserFacadeService;
import com.lifang.imsoa.model.request.AppNameEnum;
import com.lifang.imsoa.model.response.HxUserStatus;
import com.lifang.json.FasterJsonTool;
import com.lifang.mipushsoa.facade.MiPushSOAServer;
import com.lifang.mqservice.client.MsgQueueSenderClient;
import com.lifang.mqservice.model.MqMessage;
import com.lifang.siteService.rmi.RMISiteServiceService;
import com.lifang.userManager.model.HttpRequestSendShortMessageModel;
import com.lifang.userManager.model.HttpRequestUserLoginModel;
import com.lifang.userManager.model.HttpResponseGuestDetailInfo;
import com.lifang.userManager.model.HttpResponseGuestInfoModel;
import com.lifang.userManager.rmi.RMIUserLoginService;
import com.lifang.userapp.base.service.BaseService;
import com.lifang.userapp.model.AreaModel;
import com.lifang.userapp.model.GuestModel;
import com.lifang.userapp.model.MQModel;
import com.lifang.userapp.model.SimpleAgent;
import com.lifang.userapp.model.SmsModel;
import com.lifang.userapp.param.*;
import com.lifang.userapp.response.GuestInfoResponse;
import com.lifang.userapp.response.OfferPriceConfigParamResponse;
import com.lifang.userapp.service.CommonServer;
import com.lifang.userapp.service.PersonConterService;
import com.lifang.userapp.util.CommonUtil;
import com.lifang.userapp.util.MQUtil;
import com.lifang.userapp.util.ResponseUtil;
import com.lifang.userapp.util.StringUtil;
import com.lifang.utils.WebTool;
import com.lifang.xmemcached.MemcachedClientAdapter;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;


// TODO: Auto-generated Javadoc
/**
 * 公共server部分.
 *
 * @author duanlsh
 */
@Service("commonServer")
public class CommonServerImpl extends BaseService implements CommonServer {

	@Autowired
	private PersonConterService personConterService;
	
	@Autowired
	private RMIHouseBidService houseBidService;
	
	@Autowired
	private RMIUserLoginService userLoginService;
	
 	@Autowired
	 private MsgQueueSenderClient mqSenderClient;
 	
	@Autowired
	private TaskExecutor taskExecutor;
	
	@Autowired
	private RMIChannelCounterService channelCounterService;
	
	@Autowired
	private ImUserFacadeService imUserFacadeService;
	
	@Autowired
	private RMISiteServiceService siteService;
	
	/** The img soa client. */
	@Autowired
	private ImageService imageService;
	
//	@Autowired
//	private IMServer iMServer;
	
	@Resource(name ="redisClientAdapter")
	private MemcachedClientAdapter redisClientAdapter;
	
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private AgentSOAServer agentSOA;
	
	@Autowired
	private CustomerAgentSOAServer customerAgentSOAServer;
	
	@Autowired
	private MiPushSOAServer miPushSOAServer;

	@Autowired
	private AgentCommonService agentCommonService;
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#findGuestInfoByGuestPhoneNum(java.lang.Long)
	 */
	public HttpResponseGuestInfoModel findGuestInfoByGuestPhoneNum(String guestTelPhoneNum) throws Exception{
		HttpRequestUserLoginModel hrulm = new HttpRequestUserLoginModel();
		hrulm.setGuestPhoneNum(guestTelPhoneNum);
		com.lifang.model.Response<String> guestInfoResponse = userLoginService.findGuestInfoByGuestPhoneNumOrGuestId(hrulm);
		String hrgimStr = FasterJsonTool.writeValueAsString(guestInfoResponse.getData());
		HttpResponseGuestInfoModel hrgim = FasterJsonTool.readValue(hrgimStr, HttpResponseGuestInfoModel.class);
		if(hrgim == null){
			throw new NullPointerException("根据客户电话获取客户id为null");
		}
		return hrgim;
	}
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#findGuestInfoByGuestPhoneNum(java.lang.Long)
	 */
	public HttpResponseGuestInfoModel findGuestInfoByGuestPhoneNum(Long guestId){
		PersonConterRequest personConterRequest = new PersonConterRequest();
		personConterRequest.setGuestId(guestId);
		ResponseUtil guestInfoResponse = personConterService.findGuestInfoByGuestPhoneNum(personConterRequest);
		String hrgimStr = FasterJsonTool.writeValueAsString(guestInfoResponse.getData());
		HttpResponseGuestInfoModel hrgim = FasterJsonTool.readValue(hrgimStr, HttpResponseGuestInfoModel.class);
		if(hrgim == null){
			throw new IllegalArgumentException("根据客户电话获取客户id为null");
		}
		return hrgim;
	}
	
	
	/**
	 * 根据guestId获取客户信息
	 * @param guestIds
	 * @return
	 * @throws Exception
	 */
	public List<HttpResponseGuestDetailInfo> findGuestInfoListsByGuestIds(List<Long> guestIds) throws Exception{
		com.lifang.model.Response<?> findGuestInfoResponse = userLoginService.findGuestInfoBatch(null, guestIds);
		String hrgimStr = FasterJsonTool.writeValueAsString(findGuestInfoResponse.getData());
		if(hrgimStr == null){
			throw new IllegalArgumentException("根据客户电话获取客户id为null");
		}
		List<HttpResponseGuestDetailInfo> hrgimLists = FasterJsonTool.readValue2List(
				hrgimStr, new com.fasterxml.jackson.core.type.TypeReference<List<HttpResponseGuestDetailInfo>>() {});
		return hrgimLists;
	}
	
	
	/**
	 * 根据guestId获取客户信息
	 * @param guestIds
	 * @return
	 * @throws Exception
	 */
	public List<GuestInfoResponse> findGuestInfoLists(List<GuestModel> guestModelLists) throws Exception{
		if (guestModelLists == null) return null;
		List<GuestInfoResponse> guestInfoLists = new ArrayList<GuestInfoResponse>();
		List<Long> guestIdLists = new ArrayList<Long>();
		for (GuestModel guestModel : guestModelLists) {
			guestIdLists.add(guestModel.getGuestId());
		}
		com.lifang.model.Response<?> findGuestInfoResponse = userLoginService.findGuestInfoBatch(null, guestIdLists);
		String hrgimStr = FasterJsonTool.writeValueAsString(findGuestInfoResponse.getData());
		if(hrgimStr == null){
			throw new NullPointerException("根据客户电话获取客户id为null");
		}
		List<HttpResponseGuestDetailInfo> hrgimLists = FasterJsonTool.readValue2List(
				hrgimStr, new com.fasterxml.jackson.core.type.TypeReference<List<HttpResponseGuestDetailInfo>>() {});
		for (HttpResponseGuestDetailInfo guestDetailInfo : hrgimLists) {
			GuestInfoResponse guestInfo = new GuestInfoResponse();
			guestInfo.setGuestId(guestDetailInfo.getPkid().longValue());
			guestInfo.setPhoneNum(guestDetailInfo.getPhoneNum());
			guestInfo.setSex(guestDetailInfo.getSex());
			guestInfo.setName(guestDetailInfo.getName());
			guestInfo.setLoginKey(guestDetailInfo.getLoginKey());
			guestInfo.setPushRecognition(guestDetailInfo.getPkid().toString());
			guestInfo.setGuestPhotoUrl(guestDetailInfo.getAvatar() == null ? "" : guestDetailInfo.getAvatar().getUrl());
			//客户级别信息
			for (GuestModel guestModel : guestModelLists) {
				if (guestDetailInfo.getPkid() != null && guestDetailInfo.getPkid() == guestModel.getGuestId().intValue()) {
					guestInfo.setGuestLevel(guestModel.getLevel());
					break;
				}
			}
			List<HxUserStatus> hxUserLists = this.changeByAgentIdAndHxAgentId(Arrays.asList(guestDetailInfo.getPkid().toString()), 2, AppNameEnum.WKZF);
			if (hxUserLists != null && hxUserLists.size() > 0) {
				guestInfo.setImGuestId(hxUserLists.get(0).getHxId());
			}
			guestInfoLists.add(guestInfo);
		}
		return guestInfoLists;
	}

	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#findOfferPriceAndIntentGold(com.lifang.userapp.param.OfferPriceRequest)
	 */
	public HttpResponseHouseBidRecordDetailInfoModel findOfferPriceAndIntentGold(OfferPriceRequest offerPriceRequest) throws Exception{
		HttpRequestHouseBidModel hrhbm = new HttpRequestHouseBidModel();
		if(offerPriceRequest.getPreBidId() != 0){
			hrhbm.setPreBidId(offerPriceRequest.getPreBidId());
		}else if(StringUtils.isNotBlank(offerPriceRequest.getOfferPriceId())){
			hrhbm.setPaymentCode(offerPriceRequest.getOfferPriceId());
		}else{
			return null;
		}
		com.lifang.model.Response<String> houseBidInfoResponse = houseBidService.findHouseBidInfoById(hrhbm);
		String houseBidStr = FasterJsonTool.writeValueAsString(houseBidInfoResponse.getData());
		HttpResponseHouseBidRecordDetailInfoModel hrhbdm = FasterJsonTool.readValue(houseBidStr, HttpResponseHouseBidRecordDetailInfoModel.class);
		return hrhbdm;
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#findGuestBidHouseDetailByHouseIdAndGuestId(com.lifang.userapp.param.OfferPriceRequest)
	 */
	public HttpResponseHouseBidRecordDetailInfoModel findGuestBidHouseDetailByHouseIdAndGuestId(OfferPriceRequest offerPriceRequest) throws Exception{
		HttpRequestHouseBidOperateModel hrhbom = new HttpRequestHouseBidOperateModel();
		hrhbom.setHouseId(offerPriceRequest.getHouseId());
		hrhbom.setGuestId(offerPriceRequest.getGuestId());
		com.lifang.model.Response<?> findGuestBidInfoResponse = houseBidService.findGuestBidHouseDetailByHouseIdAndGuestId(hrhbom);
		String houseBidStr = FasterJsonTool.writeValueAsString(findGuestBidInfoResponse.getData());
		HttpResponseHouseBidRecordDetailInfoModel hrhbdm = FasterJsonTool.readValue(houseBidStr, HttpResponseHouseBidRecordDetailInfoModel.class);
		return hrhbdm;
	}
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#getOfferPriceConfigParam(com.lifang.userapp.param.OfferPriceRequest)
	 */
	public OfferPriceConfigParamResponse getOfferPriceConfigParam(OfferPriceRequest offerPriceRequest) throws Exception{
		OfferPriceConfigParamResponse offerPriceConfigParamResponse = new OfferPriceConfigParamResponse();
		//从数据库中获取相应的信息配置信息
		com.lifang.model.Response<String> hrhbrmResponse = houseBidService.findHouseBidCashRule();
		String hrhbrmStr = FasterJsonTool.writeValueAsString(hrhbrmResponse.getData());
		ObjectMapper objectMapper = new ObjectMapper();
		List<HttpResponseHouseBidRuleModel> hrhbrmList = objectMapper.readValue(hrhbrmStr, new TypeReference<List<HttpResponseHouseBidRuleModel>>(){});
		if(hrhbrmList != null && hrhbrmList.size() >0 ){
			for(HttpResponseHouseBidRuleModel hrhbrm : hrhbrmList){
				//作用在出价
				if(hrhbrm.getType() == 1){
					offerPriceConfigParamResponse.setOfferPriceRateUp(hrhbrm.getUpperLimit());
					offerPriceConfigParamResponse.setOfferPriceRateLow(hrhbrm.getLowerLimit());
				}else if(hrhbrm.getType() == 2){
					//作用在意向金	
					offerPriceConfigParamResponse.setIntentGoldUp(hrhbrm.getUpperLimit().setScale(1, BigDecimal.ROUND_HALF_EVEN));
					offerPriceConfigParamResponse.setIntentGoldLow(hrhbrm.getLowerLimit().setScale(1, BigDecimal.ROUND_HALF_EVEN));
				}
			}
			
			offerPriceConfigParamResponse.setContent("出价送佣金抵用券(金额随机)");
		}
		return offerPriceConfigParamResponse;
	}
	
	
	
	public void pushToYFYK(final SendNotifactionRequest sendNotifaction){
		if(sendNotifaction == null){
			logger.info("==推送消息==> 推送的消息对象为null");
			return;
		}
		if(sendNotifaction.getTelphoneNumList() == null || sendNotifaction.getTelphoneNumList().size() <= 0){
			logger.info(" ==推送消息==》 推送的电话号码为空");
			return;
		}
		logger.info("==>  推送的参数信息为：{}",FasterJsonTool.writeValueAsString(sendNotifaction));
		Runnable runnable = new Runnable() {
			
			public void run() {
				logger.info("---------------------begin push message------------------------------");
				logger.info("==>推送消息返回的结果信息为：{}",
						miPushSOAServer.sendMessage(sendNotifaction.getTelphoneNumList(), sendNotifaction.getTitle(),sendNotifaction.getMessage(),
								sendNotifaction.getData(), 2, sendNotifaction.getMsgType(), sendNotifaction.getMsgCategory()));
				logger.info("---------------------end push message------------------------------");
			}
		};
		taskExecutor.execute(runnable);
	}
	
	
	
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#sendSMS(com.lifang.userapp.param.SendNotifactionRequest)
	 */
	public void sendSMS(SmsModel smsModel) throws Exception{
		logger.info("----------------begin  send sms-------------------");
		HttpRequestSendShortMessageModel hrssmm = new HttpRequestSendShortMessageModel();
		if(StringUtils.isBlank(smsModel.getGuestTelPhoneNum())){
			logger.info("  ==>  发送短信  电话号码不能为空");
			return;
		}
		logger.info("==> 发送短信内容为：{}",FasterJsonTool.writeValueAsString(smsModel));
		hrssmm.setPhoneNum(smsModel.getGuestTelPhoneNum());
		hrssmm.setMsgId(smsModel.getSmsMessageId());
		hrssmm.setParamStr(DataUtil.escape(smsModel.getParamStr()));
		hrssmm.setMsgSourceType(CommonUtil.MSGSOURCETYPE);
		com.lifang.model.Response<String> sendResponse = userLoginService.sendShortMessageByContentAndPhoneNumAndMsgSourceType(hrssmm);
		logger.info("发送短信的结果信息为： {}" ,FasterJsonTool.writeValueAsString(sendResponse));
		logger.info("----------------end  send sms-------------------");
	}
	
	
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#sendMqMessage(java.lang.String)
	 */
	public void sendMqMessage(MQModel mqModel){
	   	 MqMessage msg = new MqMessage();
	   	 msg.setMsgTopic(MQUtil.MSG_TOPIC);
	   	 msg.setMsgType(MQUtil.MSG_TYPE_UPDATE);
	   	 try {
	   		if(mqModel == null) {
	   			logger.info("==> 发送MQ消息内容为null");
	   			return;
	   		}
			msg.setIp(WebTool.ip2Int(InetAddress.getLocalHost().getHostAddress()));
			JSONObject json = new JSONObject();
			json.put("ids", Arrays.asList(mqModel.getParam()));
			json.put("dtype", mqModel.getDtype());
			if(mqModel.getStatus() != 0) {
				json.put("status", mqModel.getStatus());
			}
			//获取guestTelPhoneNum
			HttpResponseGuestInfoModel hrgim = this.findGuestInfoByGuestPhoneNum(mqModel.getGuestId());
			if(StringUtils.isNotBlank(hrgim.getPhoneNum())) {
				json.put("guestTelPhoneNum", hrgim.getPhoneNum());
			}
			msg.setMemo(json.toString());
			boolean sendResult = mqSenderClient.sendMessage(msg);
			logger.info("==>发送MQ消息返回的结果信息为：{}",sendResult);
		} catch (UnknownHostException e) {
			 logger.error("获取本机ip异常！",e);
		}
   }
	
	/* (non-Javadoc)
	 * @see com.lifang.userapp.service.CommonServer#getIosChannel(java.lang.String)
	 */
	public String getIosChannel(String deviceId)
	{
		String channel = "";
		try
		{
			//从内存中获取信息
			String iosIpStr = StringUtil.getAppIp(request);
			
			logger.info("==》 StatisticsCountAOP 获取ip信息为：{}", iosIpStr);
			String memcacheKey_channel = CommonUtil.WEB_IOS_MEMCACHEDKEY_CHANNEL + iosIpStr;
			
			logger.info("==》 StatisticsCountAOP 获取前端ip地址拼接的 web_ios_memcachedkey_channel 信息为：{}", memcacheKey_channel);
			
			Object iosAdviceChannel = redisClientAdapter.get(memcacheKey_channel);
			
			if (iosAdviceChannel == null) {
				memcacheKey_channel = CommonUtil.WAP_IOS_MEMCACHEDKEY_CHANNEL+iosIpStr;
				logger.info("==》 StatisticsCountAOP 获取前端ip地址拼接的 wap_ios_memcachedkey_channel 信息为：{}", memcacheKey_channel);
				
				iosAdviceChannel = redisClientAdapter.get(memcacheKey_channel);
				if (iosAdviceChannel == null) 
				{
					HttpRequestChannelCounterModel hrccm = new HttpRequestChannelCounterModel();
					hrccm.setDeviceName(DataUtil.escape(deviceId));
					hrccm.setSystemName(CommonUtil.DEVICE_IOS);
					com.lifang.model.Response<String> iosChannelResponse = channelCounterService.findIosBindChannelKeyByDeviceId(hrccm);
					if (iosChannelResponse.getStatus() ==1 && iosChannelResponse.getData() != null) 
					{
						iosAdviceChannel = iosChannelResponse.getData();
					}
				}
			} 
			
			if (iosAdviceChannel != null) {
				channel = iosAdviceChannel.toString();
			}
			logger.info("==>  ios 统计信息 渠道 channel：{}", channel);
		}
		catch(Exception e)
		{
			logger.info("==》  ios获取渠道信息异常");
		}
		return channel ;
	}
	
	
	/////////IM信息////////////////////////////
	
	/**
	 * 根据 环信id获取经纪人id信息 和有效状态信息
	 * @param imAgentIdLists
	 * @param status 2 表示根据经纪人id获取环信id   1表示根据环信id获取经纪人id
	 * @return
	 */
	public List<HxUserStatus> changeByAgentIdAndHxAgentId (List<String> idLists, int status, AppNameEnum appName) {
		//环信id转换为id
		if (status == 1) {
			com.lifang.model.Response<?> agentIdByHxIdResponse = imUserFacadeService.getUserIdByHxId(idLists, appName);
			logger.info("==》 环信id转换为经纪人id的时候，返回的结果为：{}", FasterJsonTool.writeValueAsString(agentIdByHxIdResponse));
			String imAccountStr = FasterJsonTool.writeValueAsString(agentIdByHxIdResponse.getData());
			List<HxUserStatus> userStatusLists = FasterJsonTool.readValue2List(imAccountStr, 
					new com.fasterxml.jackson.core.type.TypeReference<List<HxUserStatus>>(){});
			return userStatusLists;
		}
		
		//正常id转换为环信id
		else if (status == 2) {
			com.lifang.model.Response<?> hxIdByAgentIdResponse = imUserFacadeService.getHxIdByUserId(idLists, appName);
			logger.info("==》 经纪人id转换为环信id：{}", FasterJsonTool.writeValueAsString(hxIdByAgentIdResponse));
			String accountStr = FasterJsonTool.writeValueAsString(hxIdByAgentIdResponse.getData());
			List<HxUserStatus> hxUserStatusLists = FasterJsonTool.readValue2List(accountStr, 
					new com.fasterxml.jackson.core.type.TypeReference<List<HxUserStatus>>() {});
			return hxUserStatusLists;
		}
		
		else {
			logger.info("当前传递的状态有误");
			return null;
		}
	}
	
	
	/////////经纪人信息//////////////////////
	
	
	/**
	 * 经纪人转换为简单经纪人
	 * @param agent
	 * @return
	 */
	public SimpleAgent changeAgentToSimpleAgent(Agent agent, int agentType, List<ResponseDistrictAndTown> agentKnowDisAndTown, int systemAgentType) {
		if (agent == null) {
			throw new IllegalArgumentException("传递的经纪人信息不能为空");
		}
		if (systemAgentType != CommonUtil.HOUSE_SOURCE_SELF && systemAgentType != CommonUtil.HOUSE_SOURCE_YFYK) {
			throw new IllegalArgumentException("经纪人类型不能为空");
		}
		String simpleAgentStr = FasterJsonTool.writeValueAsString(agent);
		SimpleAgent simpleAgent = FasterJsonTool.readValue(simpleAgentStr, SimpleAgent.class);
		
		//经纪人id转换为环信id
		if (agent.getId() != null && agent.getId() > 0) {
			List<HxUserStatus> hxUserStatus = this.changeByAgentIdAndHxAgentId(Arrays.asList(agent.getId().toString()), 2, AppNameEnum.YFYK);
			if (hxUserStatus != null && hxUserStatus.size() > 0) {
				simpleAgent.setImAgentId(hxUserStatus.get(0).getHxId());
				simpleAgent.setImAgentIsValid(hxUserStatus.get(0).getStatus() == 1 ? 1 : 0);
			}
		}
		
		//经纪人类型
		simpleAgent.setSystemAgentType(systemAgentType);
		
		//经纪人头像信息
		if (StringUtils.isNotBlank(agent.getPhotoHeadKey())) {
			ImageKeyObject imageKeyObj = this.getHeaderImgByKey(agent.getPhotoHeadKey());
			if (imageKeyObj != null) {
				simpleAgent.setPhotoHeadUrl(imageKeyObj.getOriginal());
			}
		}
		
		//经纪人好评率
		if (agent.getHighPercentage() != null) {
			simpleAgent.setHighPercentage(agent.getHighPercentage().multiply(new BigDecimal("100")).setScale(1, BigDecimal.ROUND_HALF_UP) + "%");
		}
		
		//所属公司
		if (StringUtils.isNotBlank(agent.getFranchiseeCompanyName())) {
			simpleAgent.setCompanyName(agent.getFranchiseeCompanyName());
		}
		//评论列表
		if (agent.getComments() != null && agent.getComments().size() > 0) {
			List<AgentComment> newAgentCommentLists = new ArrayList<>();
			List<AgentComment> agentCommentLists = agent.getComments();
			for (AgentComment agentComment : agentCommentLists) {
				if (StringUtils.isNotBlank(agentComment.getComment())) {
					newAgentCommentLists.add(agentComment);
				}
			}
			simpleAgent.setComments(newAgentCommentLists);
		}
		
		if (systemAgentType == CommonUtil.HOUSE_SOURCE_SELF) {
			//拼接返回结果信息
			StringBuffer orderMessageBuffer = new StringBuffer();
			orderMessageBuffer.append(agent.getName()).append("是您的自营服务经纪人，为您提供专业服务");
			simpleAgent.setServiceAreaMsg(orderMessageBuffer.toString());
		} 
		else {
			//区域板块信息
			if (agent.getTowns() != null && agent.getTowns().size() > 0) {
				List<AreaModel> distrctLists = new ArrayList<AreaModel>();
				for (DicAreaNew dicAreaNew : agent.getTowns()) {
					boolean isExists = false;
					for (AreaModel distractModel : distrctLists) {
						//区域信息相同
						if (dicAreaNew.getDistrictId().equals(distractModel.getId().toString()) && dicAreaNew.getDistrctName().equals(distractModel.getName())) {
							isExists = true;
							break;
						}
					}
					//如果不存在则把信息添加到区域列表信息
					if (!isExists){
						AreaModel distrctAreaModel = new AreaModel();
						distrctAreaModel.setId(dicAreaNew.getDistrictId());
						distrctAreaModel.setName(dicAreaNew.getDistrctName());
						distrctLists.add(distrctAreaModel);
					}
					for (AreaModel distractModel : distrctLists) {
						//区域信息相同
						if (dicAreaNew.getDistrictId().equals(distractModel.getId().toString()) && dicAreaNew.getDistrctName().equals(distractModel.getName())) {
							List<AreaModel> townLists = distractModel.getTownLists();
							if (townLists == null) {
								townLists = new ArrayList<AreaModel>();
							}
							AreaModel townAreaModel = new AreaModel();
							townAreaModel.setId(dicAreaNew.getId().toString());
							townAreaModel.setName(dicAreaNew.getName());
							townLists.add(townAreaModel);
							distractModel.setTownLists(townLists);
						}
					}
				}
				simpleAgent.setAreaModel(distrctLists);
				//拼接返回结果信息
				StringBuffer orderMessageBuffer = new StringBuffer();
				orderMessageBuffer.append(agent.getName()).append("是您");
				for (AreaModel distrctAreaModel : distrctLists) {
					orderMessageBuffer.append(" ").append(distrctAreaModel.getName());
					if (distrctAreaModel.getTownLists() != null && distrctAreaModel.getTownLists().size() > 0) {
						for (int i = 0; i < distrctAreaModel.getTownLists().size(); i++) {
							orderMessageBuffer.append(distrctAreaModel.getTownLists().get(i).getName());
							if (i != distrctAreaModel.getTownLists().size() -1) {
								orderMessageBuffer.append("、");
							}
						}
					}
				}
				orderMessageBuffer.append("的区域专属经纪人，为您提供该区域的专属购房服务。");
				simpleAgent.setServiceAreaMsg(orderMessageBuffer.toString());
				logger.info("++++++服务区域提示信息+++++{}", orderMessageBuffer.toString());
			}
		}
		
		//熟悉板块
		if (agentKnowDisAndTown == null) {
			ResponseStatus<List<ResponseDistrictAndTown>> agentBusinessAreaResponse = 
					agentCommonService.queryAgentBusinessArea(agent.getId());
			logger.info("==》获取经纪人熟悉板块返回的结果信息为：{}", FasterJsonTool.writeValueAsString(agentBusinessAreaResponse));
			if (agentBusinessAreaResponse != null && agentBusinessAreaResponse.getStatus() == 1 && agentBusinessAreaResponse.getData() != null) {
				
				simpleAgent.setKnowDicAndAreaMsg(this.getKnowDicAndTown(agentBusinessAreaResponse.getData()));
			}
		} else {
			if (agentKnowDisAndTown.size() > 0) {
				simpleAgent.setKnowDicAndAreaMsg(this.getKnowDicAndTown(agentKnowDisAndTown));
			}
		}
		
		//经纪人类型
		if (agentType != 0) {
			simpleAgent.setAgentType(agentType);
		}
		
		if (agent.getAgentLabel() == 0 || agent.getAgentLabel() == 1 || agent.getAgentLabel() == 2) {
			simpleAgent.setAgentLabel(agent.getAgentLabel());
		}
		else {
			simpleAgent.setAgentLabel(3);
		}
		return simpleAgent;
	}
	
	
	/**
	 * 根据经纪人熟悉的区域，封装板块信息
	 * @param agentBusinessAreaList
	 * @return
	 */
	private String getKnowDicAndTown(List<ResponseDistrictAndTown> agentBusinessAreaList) {
		if (agentBusinessAreaList == null || agentBusinessAreaList.size() <= 0) return null;
		
		StringBuilder knowDicAndAreaMsg = new StringBuilder();
		for (ResponseDistrictAndTown districtAndTownParam : agentBusinessAreaList) {
			
			if (knowDicAndAreaMsg.length() > 0 && (knowDicAndAreaMsg.lastIndexOf(",") + 1) == knowDicAndAreaMsg.length()) {
				knowDicAndAreaMsg = this.changeSplitToOther(knowDicAndAreaMsg);
			}
			
			if (districtAndTownParam.getTowns() != null && districtAndTownParam.getTowns().size() > 0) {
				knowDicAndAreaMsg.append(districtAndTownParam.getDistrict()).append(":");

				for(Town townParam : districtAndTownParam.getTowns()) {
					knowDicAndAreaMsg.append(townParam.getTown()).append(",");
				}
			}
		}
		
		
		knowDicAndAreaMsg = this.changeSplitToOther(knowDicAndAreaMsg);
		
		return knowDicAndAreaMsg.toString();
	}
	
	/**
	 * 更改内容信息
	 * @param knowDicAndAreaMsg
	 * @return
	 */
	private StringBuilder changeSplitToOther(StringBuilder knowDicAndAreaMsg) {
		if (knowDicAndAreaMsg.length() > 0 && (knowDicAndAreaMsg.lastIndexOf(",") + 1) == knowDicAndAreaMsg.length()) {
			knowDicAndAreaMsg = knowDicAndAreaMsg.replace(knowDicAndAreaMsg.length() - 1, knowDicAndAreaMsg.length(), ";");
		}
		return knowDicAndAreaMsg;
	}
	
	/**
	 * add by duanlsh inner 2015-11-16
	 * 根据经纪人id获取经纪人信息
	 * @param agentId
	 * @return
	 */
	public SimpleAgent getAgentById (Long agentId) {
		Agent agent = agentSOA.getAgentTowns(agentId.intValue());
		if (agent == null) {
			throw new NullPointerException("根据agentId：" + agentId + "获取经纪人信息为null");
		}
		
		return this.changeAgentToSimpleAgent(agent, 0, null, CommonUtil.HOUSE_SOURCE_YFYK);
	}
	
	/**
	 * 根据经纪人id获取经纪人和评论信息
	 * @param agentId
	 * @return
	 */
	public SimpleAgent getAgentAndCommentById(Long agentId, Long guestId) 
	{
		com.lifang.customeragentsoa.model.Agent customerAgent = customerAgentSOAServer.getGuestAppointAgentInfo(guestId.intValue(), agentId.intValue());
		logger.info("==> 获取经纪人信息和评价信息的结果为：{}", FasterJsonTool.writeValueAsString(customerAgent));
		if(customerAgent != null) {
			return this.changeAgentToSimpleAgent(customerAgent, customerAgent.getAgentType(), null, CommonUtil.HOUSE_SOURCE_YFYK);
		}
		
		return null;
	}
	
	
	/**
	 * 根据经纪人id列表和客户id获取经纪人和评论信息
	 * @param agentId
	 * @return
	 */
	public List<SimpleAgent> getAgentsAndComments(List<Integer> agentIds, Long guestId) {
		
		List<SimpleAgent> simpleAgentLists = new ArrayList<SimpleAgent>();
		//获取经纪人熟悉板块
		if (agentIds != null && agentIds.size() > 0) {
			ResponseStatus<List<ResponseDistrictAndTown>> agentBusinessAreaLists = agentCommonService.queryAgentBusinessArea(agentIds);
			logger.info("==> 批量获取经纪人{}的熟悉板块为：{}", FasterJsonTool.writeValueAsString(agentIds), FasterJsonTool.writeValueAsString(agentBusinessAreaLists));
			
			List<com.lifang.customeragentsoa.model.Agent> customerAgentLists = customerAgentSOAServer.getGuestAppointAgentInfos(guestId.intValue(), agentIds);
			logger.info("==> 根据客户id和经纪人列表获取经纪人详细信息返回的结果信息为：{}", FasterJsonTool.writeValueAsString(customerAgentLists));
			
			if(customerAgentLists != null && customerAgentLists.size() > 0) {
				for (com.lifang.customeragentsoa.model.Agent agent: customerAgentLists) {
					if (agent != null) {
						
						List<ResponseDistrictAndTown> localAgentDistrictAndTown = new ArrayList<ResponseDistrictAndTown>();
						
						if (agentBusinessAreaLists != null && agentBusinessAreaLists.getData() != null) {
							List<ResponseDistrictAndTown> responseDistrictAndTownLists = agentBusinessAreaLists.getData();
							for (ResponseDistrictAndTown districtAndTownParam : responseDistrictAndTownLists) {
								if (districtAndTownParam != null && districtAndTownParam.getAgentId() != null 
										&& agent.getId() != null && districtAndTownParam.getAgentId().intValue() == agent.getId().intValue()) {
									localAgentDistrictAndTown.add(districtAndTownParam);
								}
							}
						}
						
						
						simpleAgentLists.add(this.changeAgentToSimpleAgent(agent, agent.getAgentType(), localAgentDistrictAndTown, CommonUtil.HOUSE_SOURCE_YFYK));
					}
				}
				
			}
		}
		return simpleAgentLists;
	}
	
	
	/**
	 * 批量获取经纪人信息
	 * @param agentIdLists
	 * @return
	 */
	public List<SimpleAgent> getAgentLists (List<Integer> agentIdLists) {
		List<Agent> agentLists = agentSOA.getAgentCommentListAndTownByIds(agentIdLists);
		logger.info("==》》批量获取经纪人的信息为：{}", FasterJsonTool.writeValueAsString(agentLists));
		List<SimpleAgent> simpleAgentLists = new ArrayList<>();
		if (agentLists != null && agentLists.size() > 0) {
			for (Agent agent : agentLists) {
				if (agent != null) {
					simpleAgentLists.add(this.changeAgentToSimpleAgent(agent, 0, new ArrayList<ResponseDistrictAndTown>(), CommonUtil.HOUSE_SOURCE_YFYK));
				}
			}
		}
		return simpleAgentLists;
	}
	
	
	/**
	 * 新房中的获取经纪人信息
	 * @param agentId
	 * @param guestId
	 * @return
	 */
	public SimpleAgent getNewHouseAgentAndCommentById(Long agentId, Long guestId) {
		
		Agent agent = agentSOA.getAgentInfoHaveCommentAndTag(agentId.intValue(), 0, 10, null, null);
		
		if(agent != null)
		{
			return this.changeAgentToSimpleAgent(agent, 0, null, CommonUtil.HOUSE_SOURCE_YFYK);
		}
		
		return null;
	}
	
	
	/**
	 * 根据经纪人电话号码获取经纪人信息
	 * @param agentTelPhone
	 * @return
	 */
	public Agent getAgentByAgentTelPhoneNum (String agentTelPhoneNum) {
		return agentSOA.getAgent(agentTelPhoneNum);
	}
	
	/**
	 * 根据经纪人Id获取简单经纪人信息（不带评论，标签）
	 * @param agentId
	 * @return
	 */
	public SimpleAgent getAgentWithoutComment(Long agentId) 
	{
		Agent agent = agentSOA.getAgent(agentId.intValue());
		
		if (agent == null) 
		{
			throw new IllegalArgumentException("根据agentId：" + agentId + "获取经纪人信息为null");
		}
		
		return this.changeAgentToSimpleAgent(agent, 0, null, CommonUtil.HOUSE_SOURCE_YFYK);
	}

	
	/**
	 * 上传图片信息
	 */
	public ImageKeyObject uploadImgFile(byte [] fileByte) {
		ImageKeyObject iko = imageService.uploadSingleFileNoHandle2PubBucket(fileByte);
		return iko;
	}
	
	
	/**
	 * 获取图片接口
	 */
	public ImageKeyObject getImgFile (String key) {
		ImageKeyObject  iko = imageService.getImageKeyObject4Internal(key);
		return iko;
	}
	
	/**
	 * 获取头像信息
	 * @param key
	 * @return
	 */
	public ImageKeyObject getHeaderImgByKey (String key) {
		ImageKeyObject imageKeyObj = imageService.getImageKeyUrl(key);
		logger.debug("==》获取图片的结果信息为：{}", FasterJsonTool.writeValueAsString(imageKeyObj));
		return imageKeyObj;
	}
}
