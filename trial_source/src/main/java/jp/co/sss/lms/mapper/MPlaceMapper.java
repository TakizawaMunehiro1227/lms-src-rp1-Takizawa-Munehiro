package jp.co.sss.lms.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.PlaceDto;
import jp.co.sss.lms.dto.UserAttendanceDto;

/**
 * 会場マスタMapper
 *@author 瀧澤：-Task.58 勤怠一括登録画面
 * 
 */
@Mapper
public interface MPlaceMapper {

    /**
     * @author 瀧澤-Task.58 会場IDによる会場情報取得
     *
     * @param placeId 会場ID
     * @param hiddenFlg 非表示フラグ
     * @param deleteFlg 削除フラグ
     * @return 会場DTO
     */
    PlaceDto findByPlaceId(
            @Param("placeId") Integer placeId,
            @Param("hiddenFlg") Short hiddenFlg,
            @Param("deleteFlg") Short deleteFlg);

    /**
     * @author 瀧澤-Task.58 ユーザー勤怠情報DTO取得
     *
     * @param placeId 会場ID
     * @param from 検索開始日
     * @param to 検索終了日
     * @param deleteFlg 削除フラグ
     * @return ユーザー勤怠情報DTOリスト
     */
    List<UserAttendanceDto> getUserAttendanceDto(
            @Param("placeId") Integer placeId,
            @Param("from") Date from,
            @Param("to") Date to,
            @Param("deleteFlg") Short deleteFlg);
    
   
}