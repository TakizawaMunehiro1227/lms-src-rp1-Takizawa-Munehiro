package jp.co.sss.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.PlaceDto;
import jp.co.sss.lms.mapper.MPlaceMapper;
import jp.co.sss.lms.util.Constants;

/**
 * 会場サービス
 *
 * @author 東京ITスクール
 */
@Service
public class PlaceService {

    @Autowired
    private MPlaceMapper mPlaceMapper;

    /**
     * Task.58 会場IDによる会場情報取得
     *
     * @param placeId 会場ID
     * @return 会場DTO
     */
    public PlaceDto findByPlaceId(Integer placeId) {

        // 会場IDをキーに会場情報を取得
        return mPlaceMapper.findByPlaceId(
                placeId,
                Constants.DB_FLG_FALSE,
                Constants.DB_FLG_FALSE);
    }
}