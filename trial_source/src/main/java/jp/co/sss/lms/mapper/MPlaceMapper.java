package jp.co.sss.lms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.PlaceDto;

/**
 * 会場マスタMapper
 *
 * Task.58 勤怠一括登録画面
 */
@Mapper
public interface MPlaceMapper {

    /**
     * 会場IDによる会場情報取得
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
}