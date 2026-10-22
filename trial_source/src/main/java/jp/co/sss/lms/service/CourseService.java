package jp.co.sss.lms.service;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.CourseServiceCourseDto;
import jp.co.sss.lms.mapper.MCourseMapper;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.LoggingUtil;
import jp.co.sss.lms.util.MessageUtil;

/**
 * コース情報サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class CourseService {

	@Autowired
	private MCourseMapper mCourseMapper;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoggingUtil loggingUtil;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * パラメータチェック
	 * 
	 * @param courseId
	 * @return エラーメッセージ
	 */
	public String checkCourseId(Integer courseId) {
		Integer courseCount = mCourseMapper.getCourseCount(courseId);
		if (courseCount == 0) {
			String message = messageUtil.getMessage(Constants.VALID_KEY_ALREADYDELETE,
					new String[] { "コースID " + courseId });
			StringBuffer sb = new StringBuffer(message);
			loggingUtil.appendLog(sb);
			logger.info(sb.toString());
			return message;
		}
		return "";
	}

	/**
	 * コース情報サービス コースDTOの取得
	 * 
	 * @param courseId
	 * @return courseServiceCourseDto
	 * @throws ParseException
	 */
	public CourseServiceCourseDto getCourseDetail(Integer courseId) throws ParseException {

		CourseServiceCourseDto courseServiceCourseDto = mCourseMapper.getCourseDetail(courseId,
				Constants.DB_FLG_FALSE);

		return courseServiceCourseDto;

	}
	




}
