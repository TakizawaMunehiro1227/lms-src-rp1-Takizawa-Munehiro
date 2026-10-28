package jp.co.sss.lms.form;

import lombok.Data;

@Data
public class AttendanceSearchForm {

	//コース名
	private String courseName;
	//会社名
	private String companyName;
	//ユーザー名
	private String userName;

	// Task71：経過フラグ
	// 0：期間超過者を除外
	// 1：期間超過者を含める
	private Integer pastFlg = 0;

}
