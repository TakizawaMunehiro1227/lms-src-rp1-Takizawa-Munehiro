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

}
