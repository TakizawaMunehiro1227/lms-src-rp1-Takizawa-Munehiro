package jp.co.sss.lms.form;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Task.58 勤怠一括登録フォーム
 *
 * @author 東京ITスクール
 */
@Data
public class BulkRegistForm {

    /** 会場ID */
    private Integer placeId;

    /** 会場名 */
    private String placeName;

    /** 検索期間（開始日） */
    private String searchPeriodFrom;

    /** 検索期間（終了日） */
    private String searchPeriodTo;
   
    
 // Task.58 勤怠一覧
    private List<DailyAttendanceForm> attendanceList = new ArrayList<>();

    // Task.58 確定対象の日付インデックス
    private Integer targetIndex;
    

}