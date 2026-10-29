package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.entity.TCompanyAttendance;

@Mapper
public interface TCompanyAttendanceMapper {
	/**
	 * @author 瀧澤-Task.58 勤怠情報（企業入力）取得（企業入力勤怠情報ID）
	 * 
	 * @param companyAttendanceId
	 * @param deleteFlg
	 * @return 勤怠情報（企業入力）エンティティ
	 */
	TCompanyAttendance findByCompanyAttendanceId(
			@Param("companyAttendanceId") Integer companyAttendanceId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * @author 瀧澤-Task.58 勤怠情報（企業入力）登録
	 * 
	 * @param tCompanyAttendance
	 * @return 登録結果
	 */
	Boolean insert(TCompanyAttendance tCompanyAttendance);

	/**
	 *@author 瀧澤-Task.58 勤怠情報（企業入力）更新
	 * 
	 * @param tCompanyAttendance
	 * @return 更新結果
	 */
	Boolean update(TCompanyAttendance tCompanyAttendance);
	


}
