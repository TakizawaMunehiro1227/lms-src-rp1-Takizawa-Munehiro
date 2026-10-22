package jp.co.sss.lms.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.UserDetailDto;

/**
 * LMSユーザーマスタマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface MLmsUserMapper {

	/**
	 * ユーザー基本情報取得
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return ユーザー基本情報DTO
	 */
	UserDetailDto getUserDetail(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * Task.57 受講生情報一覧取得
	 *
	 * @return ユーザー基本情報DTOリスト
	 */
	List<UserDetailDto> getUserDetailForSearch(
			@Param("courseName") String courseName,
			@Param("companyName") String companyName,
			@Param("userName") String userName,
			@Param("teacherLmsUserId") Integer teacherLmsUserId,
			@Param("role") String role,
			@Param("deleteFlg") Short deleteFlg);

}
