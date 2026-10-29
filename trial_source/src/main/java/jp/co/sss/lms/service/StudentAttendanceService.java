package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.PlaceDto;
import jp.co.sss.lms.dto.UserAttendanceDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.entity.TCompanyAttendance;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.AttendanceSearchForm;
import jp.co.sss.lms.form.BulkRegistForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.MLmsUserMapper;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.mapper.TCompanyAttendanceMapper;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;
	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	@Autowired
	private MLmsUserMapper mLmsUserMapper;

	@Autowired
	private PlaceService placeService;

	/**
	 * Task.58 会場マスタMapper
	 */
	@Autowired
	private MPlaceMapper mPlaceMapper;
	@Autowired
	private TCompanyAttendanceMapper tCompanyAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}

	/**
	 * @author 瀧澤志大-Task25 過去日の出席未入力チェック
	 *
	 * @return 未入力がある場合true、ない場合false
	 * @throws ParseException 日付変換時の例外
	 */
	public Boolean notEnterCheck() throws ParseException {

		// 今日の日付を取得
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		// 過去日の勤怠を判定するため、現在日時の時刻部分を0時0分0秒にする
		Date trainingDate = df.parse(df.format(date));

		// 過去日の出席未入力件数を取得
		Integer notEnterCount = tStudentAttendanceMapper.notEnterCount(
				loginUserDto.getLmsUserId(),
				Constants.DB_FLG_FALSE,
				trainingDate);

		// 未入力件数が0より大きい場合trueを返す
		return notEnterCount > 0;
	}

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());

		// 時間の選択肢を設定
		attendanceForm.setHourTimes(attendanceUtil.setHourTime());
		// 分の選択肢を設定
		attendanceForm.setMinuteTimes(attendanceUtil.setMinuteTime());

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));

			//Task26
			String startTime = attendanceManagementDto.getTrainingStartTime();
			dailyAttendanceForm.setTrainingStartTime(startTime);

			// 出勤時間を時と分に分割
			TrainingTime start = new TrainingTime(startTime);
			if (start.isNotBlank()) {
				dailyAttendanceForm.setTrainingStartTimeHour(start.getHour());
				dailyAttendanceForm.setTrainingStartTimeMinute(start.getMinute());
			}

			String endTime = attendanceManagementDto.getTrainingEndTime();
			dailyAttendanceForm.setTrainingEndTime(endTime);

			// 退勤時間を時と分に分割
			TrainingTime end = new TrainingTime(endTime);
			if (end.isNotBlank()) {
				dailyAttendanceForm.setTrainingEndTimeHour(end.getHour());
				dailyAttendanceForm.setTrainingEndTimeMinute(end.getMinute());
			}

			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}

		return attendanceForm;
	}

	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

		Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
				: attendanceForm.getLmsUserId();

		// 現在の勤怠情報（受講生入力）リストを取得
		List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
				.findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

		// 入力された情報を更新用のエンティティに移し替え
		Date date = new Date();
		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 更新用エンティティ作成
			TStudentAttendance tStudentAttendance = new TStudentAttendance();
			// 日次勤怠フォームから更新用のエンティティにコピー
			BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
			// 研修日付
			tStudentAttendance
					.setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
			// 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
			for (TStudentAttendance entity : tStudentAttendanceList) {
				if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
					tStudentAttendance = entity;
					break;
				}
			}
			tStudentAttendance.setLmsUserId(lmsUserId);
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			// 出勤時刻整形
			TrainingTime trainingStartTime = null;
			trainingStartTime = new TrainingTime(dailyAttendanceForm.getTrainingStartTime());
			tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
			// 退勤時刻整形
			TrainingTime trainingEndTime = null;
			trainingEndTime = new TrainingTime(dailyAttendanceForm.getTrainingEndTime());
			tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
			// 中抜け時間
			tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
			// 遅刻早退ステータス
			if ((trainingStartTime != null || trainingEndTime != null)
					&& !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
				AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
						.getStatus(trainingStartTime, trainingEndTime);
				tStudentAttendance.setStatus(attendanceStatusEnum.code);
			}
			// 備考
			tStudentAttendance.setNote(dailyAttendanceForm.getNote());
			// 更新者と更新日時
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			// 削除フラグ
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			// 登録用Listへ追加
			tStudentAttendanceList.add(tStudentAttendance);
		}
		// 登録・更新処理
		for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
			if (tStudentAttendance.getStudentAttendanceId() == null) {
				tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				tStudentAttendance.setFirstCreateDate(date);
				tStudentAttendanceMapper.insert(tStudentAttendance);
			} else {
				tStudentAttendanceMapper.update(tStudentAttendance);
			}
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 *@author 瀧澤 Task26 出退勤時間の形式変換
	 *
	 * @param attendanceForm 勤怠フォーム
	 */
	public void formatConversion(AttendanceForm attendanceForm) {

		for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

			// 出勤時間の変換
			Integer startHour = dailyAttendanceForm.getTrainingStartTimeHour();
			Integer startMinute = dailyAttendanceForm.getTrainingStartTimeMinute();

			if (startHour != null && startMinute != null) {
				dailyAttendanceForm.setTrainingStartTime(String.format("%02d:%02d", startHour, startMinute));
			} else {
				dailyAttendanceForm.setTrainingStartTime("");
			}

			// 退勤時間の変換
			Integer endHour = dailyAttendanceForm.getTrainingEndTimeHour();
			Integer endMinute = dailyAttendanceForm.getTrainingEndTimeMinute();

			if (endHour != null && endMinute != null) {
				dailyAttendanceForm.setTrainingEndTime(String.format("%02d:%02d", endHour, endMinute));
			} else {
				dailyAttendanceForm.setTrainingEndTime("");
			}
		}
	}

	/**
	 * @author 瀧澤-Task27 入力チェック
	 * @param attendanceForm
	 * @param result
	 */
	public void updateInputCheck(
			AttendanceForm attendanceForm,
			BindingResult result) {

		List<DailyAttendanceForm> list = attendanceForm.getAttendanceList();

		for (int i = 0; i < list.size(); i++) {

			DailyAttendanceForm daily = list.get(i);

			// 備考の文字数チェック
			if (daily.getNote() != null && daily.getNote().length() > 100) {

				result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].note",
						messageUtil.getMessage(Constants.VALID_KEY_MAXLENGTH, new String[] { "備考", "100" })));
			}

			// 出勤時間の入力チェック
			Integer startHour = daily.getTrainingStartTimeHour();
			Integer startMinute = daily.getTrainingStartTimeMinute();

			if ((startHour == null) != (startMinute == null)) {

				result.addError(
						new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingStartTimeHour",
								messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "出勤時間" })));
			}

			// 退勤時間の入力チェック
			Integer endHour = daily.getTrainingEndTimeHour();
			Integer endMinute = daily.getTrainingEndTimeMinute();

			if ((endHour == null) != (endMinute == null)) {

				result.addError(new FieldError(
						result.getObjectName(), "attendanceList[" + i + "].trainingEndTimeHour",
						messageUtil.getMessage(Constants.INPUT_INVALID, new String[] { "退勤時間" })));
			}

			// 出勤時間が未入力で、退勤時間だけ入力されている場合
			if (startHour == null && startMinute == null && endHour != null && endMinute != null) {

				result.addError(
						new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingStartTimeHour",
								messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY)));
			}

			// 出勤時間が退勤時間より後になっている場合
			if (startHour != null && startMinute != null && endHour != null && endMinute != null) {

				int startTime = startHour * 60 + startMinute;
				int endTime = endHour * 60 + endMinute;

				if (startTime > endTime) {

					result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].trainingEndTimeHour",
									messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE,new String[] { String.valueOf(i) })));
				}
			}

			// 中抜け時間が勤務時間を超えていないかチェック
			if (startHour != null && startMinute != null && endHour != null && endMinute != null
					&& daily.getBlankTime() != null) {

				// 出勤時間と退勤時間を分に変換
				int startTime = startHour * 60 + startMinute;
				int endTime = endHour * 60 + endMinute;

				// 勤務時間を計算
				int workTime = endTime - startTime;

				// 中抜け時間が勤務時間を超えている場合
				if (workTime >= 0 && daily.getBlankTime() > workTime) {

					result.addError(new FieldError(result.getObjectName(), "attendanceList[" + i + "].blankTime",
							messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_BLANKTIMEERROR)));
				}
			}
		}

	}

	/**
	 * @author 瀧澤　Task.57 受講生情報一覧取得
	 *
	 * @param attendanceSearchForm 勤怠検索フォーム
	 * @return ユーザー基本情報DTOリスト
	 */
	public List<UserDetailDto> getAttendanceDetailDtoList(
			AttendanceSearchForm attendanceSearchForm) {

		// ① Formから検索条件を取得
		String courseName = attendanceSearchForm.getCourseName();
		String companyName = attendanceSearchForm.getCompanyName();
		String userName = attendanceSearchForm.getUserName();

		// Task71：企業担当者か判定
		boolean isCompany = loginUserUtil.isCompany();
		// Task71：経過フラグを取得
		// 未チェック・未指定の場合は0として扱う
		int pastFlg = isCompany && Integer.valueOf(1).equals(attendanceSearchForm.getPastFlg()) ? 1 : 0;
		attendanceSearchForm.setPastFlg(pastFlg);

		// Task71：企業担当者の場合は所属企業IDを取得
		Integer companyId = null;
		if (isCompany) {
			companyId = loginUserDto.getCompanyId();
			if (companyId == null) {
				throw new IllegalStateException("ログインユーザーの企業IDが取得できません。");
			}
		}
		// Task71：期間超過を判定する基準日
		java.sql.Date cutoffDate = null;
		if (isCompany && pastFlg == 0) {
			// プロパティから設定期間を取得
			int pastTime = Integer.parseInt(messageUtil.getMessage("setting.search.pastTime"));
			// 設定期間を引いた基準日を計算
			cutoffDate = java.sql.Date.valueOf(LocalDate.now().minusMonths(pastTime));
		}
		// ② 講師権限の場合、LMSユーザーIDを設定
		Integer teacherLmsUserId = null;
		if (loginUserUtil.isTeacher()) {
			teacherLmsUserId = loginUserDto.getLmsUserId();
		}
		// ③ 受講生情報一覧取得
		return mLmsUserMapper.getUserDetailForSearch(
				isCompany ? null : courseName,
				isCompany ? null : companyName,
				userName,
				teacherLmsUserId,
				Constants.CODE_VAL_ROLL_STUDENT,
				Constants.DB_FLG_FALSE,
				companyId,
				pastFlg,
				cutoffDate);

	}

	/**
	 * @author 瀧澤-Task57 ログインユーザーの担当会場名取得
	 *
	 * @return 担当会場名
	 */
	public String getLoginUserPlaceName() {

		// 管理者の場合は全会場と表示
		if (!loginUserUtil.isTeacher()) {
			return "全会場";
		}
		// ① ログイン講師のLMSユーザーID取得
		Integer lmsUserId = loginUserDto.getLmsUserId();
		// ② 担当会場名をDBから取得
		List<String> placeNames = mLmsUserMapper.getPlaceNamesByLmsUserId(lmsUserId);
		// ③ 会場が登録されていない場合
		if (placeNames.isEmpty()) {
			return "担当会場未設定";
		}
		// ④ 会場名を返却
		return String.join("、", placeNames);
	}

	/**
	 *@author 瀧澤　 Task.58 勤怠一括登録フォームの初期設定
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 */
	public void setBulkRegistForm(BulkRegistForm bulkRegistForm) {

		// ① ログイン情報から会場IDを取得
		Integer placeId = loginUserDto.getPlaceId();
		// ② フォームに会場IDを設定
		bulkRegistForm.setPlaceId(placeId);
		// ③ 会場情報を取得
		PlaceDto placeDto = placeService.findByPlaceId(placeId);
		// ④ 会場情報が取得できない場合
		if (placeDto == null || placeDto.getPlaceNote() == null) {
			bulkRegistForm.setPlaceName(null);
			return;
		}
		// ⑤ 備考から教室名を取得
		String[] placeNotes = placeDto.getPlaceNote().split("\\$", -1);
		// ⑥ 教室名が存在しない場合
		if (placeNotes.length < 2 || placeNotes[1].isEmpty()) {
			bulkRegistForm.setPlaceName(null);
			return;
		}

		// ⑦ 会場名（教室名）の形式で設定
		String placeName = placeDto.getPlaceName()+ "（" + placeNotes[1] + "）";
		bulkRegistForm.setPlaceName(placeName);
	}

	/**
	 * @author 瀧澤-Task.58 勤怠一括登録画面
	 * 検索時入力チェック
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @param result 入力チェック結果
	 * @throws ParseException 日付変換時の例外
	 */
	public void searchInputCheck(BulkRegistForm bulkRegistForm, BindingResult result) throws ParseException {

		// ① すでに入力エラーがある場合は終了
		if (result.hasErrors()) {
			return;
		}
		// ② 検索期間を取得
		String fromValue = bulkRegistForm.getSearchPeriodFrom();
		String toValue = bulkRegistForm.getSearchPeriodTo();
		// ③ 検索開始日の必須チェック
		if (fromValue == null || fromValue.isBlank()) {

			result.addError(new FieldError(result.getObjectName(),"searchPeriodFrom",
					messageUtil.getMessage(Constants.VALID_KEY_REQUIRED,new String[] { "期間（From）" })));

			return;
		}
		// ④ 検索終了日の必須チェック
		if (toValue == null || toValue.isBlank()) {

			result.addError(new FieldError(result.getObjectName(),"searchPeriodTo",
					messageUtil.getMessage(Constants.VALID_KEY_REQUIRED,new String[] { "期間（To）" })));
			return;
		}
		// ⑤ 日付形式チェック用フォーマッター
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
		LocalDate from;
		LocalDate to;
		// ⑥ 開始日の日付形式チェック
		try {
			from = LocalDate.parse(fromValue, formatter);
		} catch (DateTimeParseException e) {
			result.addError(new FieldError(result.getObjectName(),"searchPeriodFrom",
					messageUtil.getMessage(Constants.VALID_KEY_DATE,new String[] { "期間（From）" })));
			return;
		}
		// ⑦ 終了日の日付形式チェック
		try {
			to = LocalDate.parse(toValue, formatter);
		} catch (DateTimeParseException e) {
			result.addError(new FieldError(result.getObjectName(),"searchPeriodTo",
					messageUtil.getMessage(Constants.VALID_KEY_DATE,new String[] { "期間（To）" })));
			return;
		}

		// ⑧ 終了日がシステム日付より未来の場合
		if (to.isAfter(LocalDate.now())) {
			result.addError(new FieldError(result.getObjectName(),"searchPeriodTo",
					messageUtil.getMessage(Constants.VALID_KEY_SEARCHTORANGEERROR,new String[] { "期間（To）" })));
			return;
		}

		// ⑨ 開始日が終了日より後の場合
		if (from.isAfter(to)) {
			result.addError(new FieldError(result.getObjectName(),"searchPeriodTo",
					messageUtil.getMessage(Constants.VALID_KEY_SEARCHPERIODCOMPAREERROR,new String[] {"期間（To）","期間（From）"})));
			return;
		}
		// ⑩ 検索期間が30日を超えている場合
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > 30) {
			result.addError(new FieldError(result.getObjectName(),"searchPeriodTo",
					messageUtil.getMessage(Constants.VALID_KEY_SEARCHSETTINGOVER,new String[] {"検索期間","30日"})));
			return;
		}
	}

	/**
	 * Task.58 ユーザー勤怠情報の取得
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @throws ParseException 日付変換時の例外
	 */
	public void getUserAttendance(BulkRegistForm bulkRegistForm) throws ParseException {

		// ① 検索条件を取得
		Integer placeId = bulkRegistForm.getPlaceId();

		// Task.58 検索期間をyyyy-MM-dd形式で日付に変換
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setLenient(false);

		Date from = sdf.parse(bulkRegistForm.getSearchPeriodFrom());
		Date to = sdf.parse(bulkRegistForm.getSearchPeriodTo());

		// ② ユーザー勤怠情報DTOリストを取得
		List<UserAttendanceDto> userAttendanceDtoList = mPlaceMapper.getUserAttendanceDto(placeId, from, to,
				Constants.DB_FLG_FALSE);

		// ③ 日次勤怠フォームリストを生成
		List<DailyAttendanceForm> attendanceList = new ArrayList<>();
		Map<String, Integer> dateIndexMap = new LinkedHashMap<>();

		// ④ 取得した勤怠情報をフォームに設定
		for (UserAttendanceDto dto : userAttendanceDtoList) {
			DailyAttendanceForm daily = new DailyAttendanceForm();

			// 勤怠情報ID
			daily.setCompanyAttendanceId(dto.getCompanyAttendanceId());
			daily.setStudentAttendanceId(dto.getStudentAttendanceId());

			// ユーザー情報
			if (dto.getLmsUserId() != null) {
				daily.setLmsUserId(String.valueOf(dto.getLmsUserId()));
			}
			daily.setUserName(dto.getUserName());
			daily.setCourseName(dto.getCourseName());

			// 研修日付
			String trainingDate = dateUtil.toString(dto.getTrainingDate());
			daily.setTrainingDate(trainingDate);
			daily.setDispTrainingDate(dateUtil.dateToString(dto.getTrainingDate(), "yyyy年M月d日(E)"));

			// 日付ごとのインデックスを設定
			if (!dateIndexMap.containsKey(trainingDate)) {
				dateIndexMap.put(trainingDate, dateIndexMap.size());
			}
			daily.setIndex(String.valueOf(dateIndexMap.get(trainingDate)));

			// 受講生の出退勤時刻
			String startTime = dto.getTrainingStartTime();
			String endTime = dto.getTrainingEndTime();

			daily.setTrainingStartTimeRaw(startTime == null || startTime.isBlank() ? Constants.NOT_ENTERED : startTime);
			daily.setTrainingEndTimeRaw(endTime == null || endTime.isBlank() ? Constants.NOT_ENTERED : endTime);

			// コピー用時刻（15分単位）
			daily.setTrainingStartTimeRounded(roundBulkAttendanceTime(startTime, true));
			daily.setTrainingEndTimeRounded(roundBulkAttendanceTime(endTime, false));

			// 中抜け時間
			daily.setBlankTime(dto.getBlankTime());
			if (dto.getBlankTime() != null) {
				daily.setBlankTimeValue(String.valueOf(attendanceUtil.calcBlankTime(dto.getBlankTime())));
			}

			// 勤怠ステータス
			if (dto.getStatus() != null) {
				daily.setStatus(String.valueOf(dto.getStatus()));
				AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
				if (statusEnum != null) {
					daily.setStatusDispName(statusEnum.name);
				}
			}

			// 備考・企業入力の初期値
			daily.setNote(dto.getNote());
			daily.setTrainingStartTime("");
			daily.setTrainingEndTime("");
			daily.setIsAbsent(false);
			attendanceList.add(daily);
		}

		// ⑤ 勤怠一括登録フォームに設定
		bulkRegistForm.setAttendanceList(attendanceList);
	}

	/**
	 * Task.58 コピー用時刻の15分単位調整
	 *
	 * @param time 受講生が入力した時刻
	 * @param isStart true：出勤、false：退勤
	 * @return 調整後の時刻
	 */
	private String roundBulkAttendanceTime(String time, boolean isStart) {

		// 未入力チェック
		if (time == null || time.isBlank() || Constants.NOT_ENTERED.equals(time)) {
			return "";
		}

		String[] parts = time.split(":");
		if (parts.length != 2) {
			return "";
		}

		int hour;
		int minute;
		try {
			hour = Integer.parseInt(parts[0]);
			minute = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			return "";
		}

		// 時刻の範囲チェック
		if (hour < 0 || hour > 24 || minute < 0 || minute > 59
				|| (hour == 24 && minute != 0)) {
			return "";
		}

		int totalMinutes = hour * 60 + minute;
		int roundedMinutes;

		if (isStart) {
			// 出勤：15分単位で切り上げ、09:00以降
			roundedMinutes = ((totalMinutes + 14) / 15) * 15;
			roundedMinutes = Math.max(roundedMinutes, 9 * 60);
		} else {
			// 退勤：15分単位で切り下げ、18:00以前
			roundedMinutes = (totalMinutes / 15) * 15;
			roundedMinutes = Math.min(roundedMinutes, 18 * 60);
		}

		return String.format("%02d:%02d",
				roundedMinutes / 60, roundedMinutes % 60);
	}

	/**
	 * Task.58 勤怠一括登録 入力チェック
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @param result 入力チェック結果
	 */
	public void bulkRegistInputCheck(
			BulkRegistForm bulkRegistForm,
			BindingResult result) {

		// ① 勤怠一覧と確定対象の日付インデックスを取得
		List<DailyAttendanceForm> attendanceList = bulkRegistForm.getAttendanceList();
		Integer targetIndex = bulkRegistForm.getTargetIndex();

		if (attendanceList == null || attendanceList.isEmpty()|| targetIndex == null) {
			result.reject("haserror");
			return;
		}

		String target = String.valueOf(targetIndex);
		boolean found = false;

		// ② 確定対象の日付のみチェック
		for (DailyAttendanceForm attendance : attendanceList) {
			if (attendance == null) {
				result.reject("haserror");
				continue;
			}
			// 確定対象の日付以外は処理しない
			if (!target.equals(attendance.getIndex())) {
				continue;
			}

			found = true;

			String trainingDate = attendance.getTrainingDate();
			String startTime = attendance.getTrainingStartTime();
			String endTime = attendance.getTrainingEndTime();

			boolean isAbsent = Boolean.TRUE.equals(attendance.getIsAbsent());
			boolean hasStart = startTime != null&& !startTime.trim().isEmpty();
			boolean hasEnd = endTime != null&& !endTime.trim().isEmpty();

			// ③ 欠席の場合
			if (isAbsent) {
				// 欠席なのに時刻が入力されている
				if (hasStart || hasEnd) {
					result.reject("absentAndTrainingTimeExistsBulk",new Object[] { trainingDate },null);
				}
				continue;
			}
			// ④ 出退勤時間が両方未入力
			if (!hasStart && !hasEnd) {
				result.reject("requiredTrainingTimeBulk",new Object[] { trainingDate },null);
				continue;
			}
			// ⑤ 出退勤時間の片方が未入力
			if (!hasStart || !hasEnd) {
				result.reject("attendance.punchInEmptyBulk",new Object[] { trainingDate },null);
				continue;
			}
			// ⑥ 時刻形式チェック
			Integer startMinutes = parseTimeMinutes(startTime);
			Integer endMinutes = parseTimeMinutes(endTime);

			if (startMinutes == null || endMinutes == null) {
				result.reject("trainingTimeBulk",new Object[] { trainingDate },null);
				continue;
			}
			// ⑦ 24:00を超えていないかチェック
			boolean isOver24 = false;

			if (startMinutes > 1440) {
				result.reject("maxvalBulk",new Object[] {trainingDate, "出勤時間", "24:00"},null);isOver24 = true;
			}

			if (endMinutes > 1440) {
				result.reject(
						"maxvalBulk",
						new Object[] {trainingDate, "退勤時間", "24:00"},null);isOver24 = true;
			}
			if (isOver24) {
				continue;
			}
			// ⑧ 出勤時間・退勤時間の前後関係チェック
			if (startMinutes >= endMinutes) {
				result.reject("attendance.trainingTimeRangeBulk",new Object[] { trainingDate },null);
			}
		}

		// ⑨ 確定対象が存在しない場合
		if (!found) {
			result.reject("haserror");
		}
	}

	/**
	 * HH:mm形式を分に変換
	 *
	 * @param time 時刻
	 * @return 分。不正な場合はnull
	 */
	private Integer parseTimeMinutes(String time) {

		if (time == null ||
				!time.trim().matches("[0-9]{2}:[0-9]{2}")) {
			return null;
		}

		String[] parts = time.trim().split(":");

		int hour = Integer.parseInt(parts[0]);
		int minute = Integer.parseInt(parts[1]);

		if (minute > 59) {
			return null;
		}

		return hour * 60 + minute;
	}

	/**
	 *@author 瀧澤 Task.58 勤怠一括登録処理
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @return 完了メッセージ
	 * @throws ParseException 日付変換時の例外
	 */
	@Transactional(rollbackFor = Exception.class)
	public String bulkUpdate(BulkRegistForm bulkRegistForm)throws ParseException {

		// ① 勤怠情報（企業入力）リストを生成
		List<TCompanyAttendance> companyAttendanceList = new ArrayList<>();
		Date date = new Date();
		// ② 確定対象の日付インデックスを取得
		Integer targetIndex = bulkRegistForm.getTargetIndex();
		if (targetIndex == null|| bulkRegistForm.getAttendanceList() == null|| bulkRegistForm.getAttendanceList().isEmpty()) {
			throw new IllegalArgumentException("確定対象の勤怠情報がありません。");
		}
		String target = String.valueOf(targetIndex);
		
		// ③ 勤怠リストの件数分繰り返す
		for (DailyAttendanceForm daily : bulkRegistForm.getAttendanceList()) {
			if (daily == null) {
				throw new IllegalArgumentException("勤怠情報が不正です。");
			}
			// 確定対象の日付のみ処理
			if (!target.equals(daily.getIndex())) {
				continue;
			}
			
			TCompanyAttendance attendance;

			// ④ 企業入力勤怠情報IDが設定されている場合
			if (daily.getCompanyAttendanceId() != null) {
				// 既存の勤怠情報を取得
				attendance = tCompanyAttendanceMapper.findByCompanyAttendanceId(daily.getCompanyAttendanceId(),Constants.DB_FLG_FALSE);

				if (attendance == null) {
					throw new IllegalStateException("更新対象の勤怠情報が存在しません。");
				}

			} else {
				// ⑤ 新規登録の場合
				attendance = new TCompanyAttendance();
				attendance.setLmsUserId(Integer.valueOf(daily.getLmsUserId()));
				attendance.setTrainingDate(dateUtil.parse(daily.getTrainingDate()));
				attendance.setAccountId(loginUserDto.getAccountId());
				attendance.setDeleteFlg(Constants.DB_FLG_FALSE);
				attendance.setFirstCreateUser(loginUserDto.getLmsUserId());
				attendance.setFirstCreateDate(date);
			}

			// ⑥ 出退勤時間・勤怠ステータスを設定
			boolean isAbsent = Boolean.TRUE.equals(daily.getIsAbsent());

			if (isAbsent) {
				// 欠席の場合
				attendance.setTrainingStartTime("");
				attendance.setTrainingEndTime("");
				attendance.setStatus(AttendanceStatusEnum.ABSENT.code);

			} else {
				TrainingTime startTime = new TrainingTime(daily.getTrainingStartTime());
				TrainingTime endTime = new TrainingTime(daily.getTrainingEndTime());
				attendance.setTrainingStartTime(startTime.getFormattedString());
				attendance.setTrainingEndTime(endTime.getFormattedString());
				// 勤怠Utilから勤怠状態を取得
				AttendanceStatusEnum status = attendanceUtil.getStatus(startTime, endTime);

				if (status == null) {
					throw new IllegalStateException("勤怠状態を判定できません。");
				}

				attendance.setStatus(status.code);
			}

			// ⑦ 最終更新者・最終更新日時を設定
			attendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			attendance.setLastModifiedDate(date);
			// ⑧ 勤怠情報リストに追加
			companyAttendanceList.add(attendance);
		}

		// 確定対象がない場合
		if (companyAttendanceList.isEmpty()) {
			throw new IllegalArgumentException("確定対象の勤怠情報がありません。");
		}

		// ⑨ 勤怠情報（企業入力）を登録・更新
		for (TCompanyAttendance attendance : companyAttendanceList) {

			Boolean updateResult;

			if (attendance.getCompanyAttendanceId() != null) {
               // UPDATE
				updateResult = tCompanyAttendanceMapper.update(attendance);

			} else {
				// INSERT
				updateResult = tCompanyAttendanceMapper.insert(attendance);
			}

			if (!Boolean.TRUE.equals(updateResult)) {
				throw new IllegalStateException("勤怠情報の登録・更新に失敗しました。");
			}
		}
		// ⑩ 完了メッセージ
		return messageUtil.getMessage("regist.complete",new String[] { "勤怠情報" });
	}

}
