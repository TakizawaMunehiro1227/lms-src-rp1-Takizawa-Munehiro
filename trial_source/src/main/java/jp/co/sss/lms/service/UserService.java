package jp.co.sss.lms.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.lms.dto.DailyReportDto;
import jp.co.sss.lms.dto.ExamResultDto;
import jp.co.sss.lms.dto.LmsUserDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.MUser;
import jp.co.sss.lms.mapper.MLmsUserMapper;
import jp.co.sss.lms.mapper.MUserMapper;
import jp.co.sss.lms.mapper.TDailyReportSubmitMapper;
import jp.co.sss.lms.mapper.TExamResultMapper;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.LoginUserUtil;

/**
 * ユーザー情報サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class UserService {

	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private HttpSession session;
	@Autowired
	private MLmsUserMapper mLmsUserMapper;
	@Autowired
	private MUserMapper mUserMapper;
	@Autowired
	private TDailyReportSubmitMapper tDailyReportSubmitMapper;
	@Autowired
	private TExamResultMapper tExamResultMapper;
	



	/**
	 * セキュリティ同意フラグ登録
	 */
	public void updateSecurityFlg() {
		Date today = new Date();
		MUser mUser = mUserMapper.findByUserId(loginUserDto.getUserId(), Constants.DB_FLG_FALSE);
		mUser.setSecurityAgreeFlg(Constants.CODE_VAL_SECURITY_AGREE);
		mUser.setLastModifiedUser(loginUserDto.getUserId());
		mUser.setLastModifiedDate(today);
		boolean updateFlg = mUserMapper.updateSecrityFlg(mUser);
		if (updateFlg) {
			loginUserDto.setSecurityAgreeFlg(Constants.CODE_VAL_SECURITY_AGREE);
			session.setAttribute("loginUserDto", loginUserDto);
		}
	}

	/**
	 * ユーザー詳細DTOの取得
	 * 
	 * @return ユーザー詳細DTO
	 */
	public LmsUserDto getUserDetail(Integer lmsUserId) {

		lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId() : lmsUserId;
		LmsUserDto lmsUserDto = new LmsUserDto();

		UserDetailDto userDetailDto = mLmsUserMapper.getUserDetail(lmsUserId,
				Constants.DB_FLG_FALSE);
		lmsUserDto.setUserDetailDto(userDetailDto);

		List<ExamResultDto> examResultDtoList = tExamResultMapper.getExamResultDto(lmsUserId,
				loginUserDto.getAccountId(), Constants.DB_FLG_FALSE);
		lmsUserDto.setExamResultDtoList(examResultDtoList);

		List<DailyReportDto> dailyReportDtoList = tDailyReportSubmitMapper
				.getDailyReportSubmitList(lmsUserId, Constants.DB_FLG_FALSE);
		lmsUserDto.setDailyReportDtoList(dailyReportDtoList);

		return lmsUserDto;
	}

	/**
	 * 瀧澤-Task72
	 * LMSユーザー基本情報取得
	 *
	 * @param lmsUserId LMSユーザーID
	 * @return ユーザー基本情報DTO
	 */
	public UserDetailDto getUserBasicInfo(Integer lmsUserId) {

		return mLmsUserMapper.getUserDetail(
				lmsUserId,
				Constants.DB_FLG_FALSE);
	}

}
