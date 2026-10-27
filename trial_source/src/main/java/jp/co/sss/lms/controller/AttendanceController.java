package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.dto.UserDetailDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.AttendanceSearchForm;
import jp.co.sss.lms.form.BulkRegistForm;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.Constants;

/**
 * 勤怠管理コントローラ
 * 
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	@Autowired
	private StudentAttendanceService studentAttendanceService;
	@Autowired
	private LoginUserDto loginUserDto;
	
	

	/**
	 * 勤怠管理画面 初期表示
	 * 
	 * @param lmsUserId
	 * @param courseId
	 * @param model
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */

	@RequestMapping(path = "/detail", method = RequestMethod.GET)
	public String index(Model model) throws ParseException {

		// 勤怠一覧の取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

		// 瀧澤-Task25 過去日の出席未入力チェック
		Boolean notEnterFlg = studentAttendanceService.notEnterCheck();
		model.addAttribute("notEnterFlg", notEnterFlg);
		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『出勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
	public String punchIn(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchIn();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);
		

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『退勤』ボタン押下
	 * 
	 * @param model
	 * @return 勤怠管理画面
	 */
	@RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
	public String punchOut(Model model) {

		// 更新前のチェック
		String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
		model.addAttribute("error", error);
		// 勤怠登録
		if (error == null) {
			String message = studentAttendanceService.setPunchOut();
			model.addAttribute("message", message);
		}
		// 一覧の再取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);
		

		return "attendance/detail";
	}

	/**
	 * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
	 * 
	 * @param model
	 * @return 勤怠情報直接変更画面
	 */
	@RequestMapping(path = "/update")
	public String update(Model model) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = studentAttendanceService
				.getAttendanceManagement(loginUserDto.getCourseId(), loginUserDto.getLmsUserId());
		// 勤怠フォームの生成
		AttendanceForm attendanceForm = studentAttendanceService
				.setAttendanceForm(attendanceManagementDtoList);
		model.addAttribute("attendanceForm", attendanceForm);

		return "attendance/update";
	}

	/**
	 * 勤怠情報直接変更画面 『更新』ボタン押下
	 * 
	 * @param attendanceForm
	 * @param model
	 * @param result
	 * @return 勤怠管理画面
	 * @throws ParseException
	 */
	@RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
	public String complete(@ModelAttribute("attendanceForm") AttendanceForm attendanceForm,
	        BindingResult result,Model model) throws ParseException {

		// Task27 入力チェック
		if (!result.hasErrors()) {
		    studentAttendanceService.updateInputCheck(attendanceForm, result);
		}

	    // 入力エラーがある場合
	    if (result.hasErrors()) {

	        // 時間などの選択肢を再設定
	        List<AttendanceManagementDto> list =
	                studentAttendanceService.getAttendanceManagement(
	                        loginUserDto.getCourseId(),
	                        loginUserDto.getLmsUserId());

	        AttendanceForm options =
	                studentAttendanceService.setAttendanceForm(list);

	        attendanceForm.setHourTimes(options.getHourTimes());
	        attendanceForm.setMinuteTimes(options.getMinuteTimes());
	        attendanceForm.setBlankTimes(options.getBlankTimes());

	        model.addAttribute("attendanceForm", attendanceForm);

	        return "attendance/update";
	    }

	    // Task26 時刻の形式変換
	    studentAttendanceService.formatConversion(attendanceForm);

	    // 更新
	    String message = studentAttendanceService.update(attendanceForm);
	    model.addAttribute("message", message);

	    // 一覧の再取得
	    List<AttendanceManagementDto> attendanceManagementDtoList =
	            studentAttendanceService.getAttendanceManagement(
	                    loginUserDto.getCourseId(),
	                    loginUserDto.getLmsUserId());

	    model.addAttribute("attendanceManagementDtoList",
	            attendanceManagementDtoList);

	    return "attendance/detail";
	}
	
	  /**
     * 初期表示・検索処理  
     *@author 瀧澤：Task.57 勤怠情報確認（受講生一覧）
     * @param attendanceSearchForm 勤怠検索フォーム
     * @param model モデル
     * @return 勤怠情報確認画面
     */
	
	@RequestMapping("/list")
	public String list(@ModelAttribute("attendanceSearchForm")AttendanceSearchForm attendanceSearchForm,Model model) {

	    // ① 受講生情報一覧取得
	    List<UserDetailDto> userDetailDtoList =studentAttendanceService.getAttendanceDetailDtoList(attendanceSearchForm);

	    // ② ログインユーザーの担当会場名を取得
	    String placeName =studentAttendanceService.getLoginUserPlaceName();

	    // ③ 受講生情報一覧をModelに設定
	    model.addAttribute("userDetailDtoList", userDetailDtoList);

	    // ④ 会場名をModelに設定
	    model.addAttribute("placeName", placeName);

	    // ⑤ 勤怠情報確認（受講生一覧）画面を表示
	    return "attendance/list";
	}
	

/**
 * Task.58 勤怠一括登録画面 初期表示
 */
@RequestMapping(path = "/bulkRegist",method = RequestMethod.GET)
public String bulkRegist(@ModelAttribute("bulkRegistForm")BulkRegistForm bulkRegistForm,Model model) throws ParseException {

    // ① 会場情報の設定
    studentAttendanceService.setBulkRegistForm(bulkRegistForm);

    // ② 確定後、検索条件がある場合は再検索
    if (bulkRegistForm.getSearchPeriodFrom() != null
            && !bulkRegistForm.getSearchPeriodFrom().isBlank()
            && bulkRegistForm.getSearchPeriodTo() != null
            && !bulkRegistForm.getSearchPeriodTo().isBlank()) {

        studentAttendanceService.getUserAttendance(
                bulkRegistForm);
    }

    // ③ フォームをModelに設定
    model.addAttribute("bulkRegistForm", bulkRegistForm);

    // ④ 勤怠一括登録画面を表示
    return "attendance/bulkRegist";
}

	
	/**
	 * Task.58 勤怠一括登録画面
	 * 検索ボタン押下処理
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @param result 入力チェック結果
	 * @param model モデル
	 * @return 勤怠一括登録画面
	 * @throws ParseException 日付変換時の例外
	 */
	@RequestMapping(path = "/bulkRegist/search", method = RequestMethod.POST)
	public String bulkRegistSearch(@ModelAttribute("bulkRegistForm") BulkRegistForm bulkRegistForm,
	        BindingResult result,Model model) throws ParseException {

	    // ① Task.58 会場情報の設定
	    studentAttendanceService.setBulkRegistForm(bulkRegistForm);

	    // ② Task.58 検索時の入力チェック
	    studentAttendanceService.searchInputCheck(bulkRegistForm, result);

	    // ③ 入力エラーがある場合
	    if (result.hasErrors()) {
	        return "attendance/bulkRegist";
	    }

	    // ④ Task.58 ユーザー勤怠情報の取得
	    studentAttendanceService.getUserAttendance(bulkRegistForm);

	    // ⑤ 勤怠一括登録画面を表示
	    return "attendance/bulkRegist";
	}
	
	/**
	 * Task.58 勤怠一括登録画面
	 * 『確定する』ボタン押下処理
	 *
	 * @param bulkRegistForm 勤怠一括登録フォーム
	 * @param result 入力チェック結果
	 * @param redirectAttributes リダイレクト属性
	 * @return 勤怠一括登録画面
	 * @throws ParseException 日付変換時の例外
	 */
	@RequestMapping(path = "/bulkRegist/complete", method = RequestMethod.POST)
	public String bulkRegistComplete(@ModelAttribute("bulkRegistForm") BulkRegistForm bulkRegistForm,
	        BindingResult result,RedirectAttributes redirectAttributes) throws ParseException {

	    // ① 入力チェック
	    studentAttendanceService.bulkRegistInputCheck(bulkRegistForm, result);

	    // ② エラーがある場合
	    if (result.hasErrors()) {
	        studentAttendanceService.setBulkRegistForm(bulkRegistForm);
	        return "attendance/bulkRegist";
	    }

	    // ③ 一括登録処理
	    String message = studentAttendanceService.bulkUpdate(bulkRegistForm);

	    // ④ 完了メッセージを設定
	    redirectAttributes.addFlashAttribute("message", message);
	    redirectAttributes.addFlashAttribute("bulkRegistForm", bulkRegistForm);

	    // ⑤ 再表示
	    return "redirect:/attendance/bulkRegist";
	}
	


}