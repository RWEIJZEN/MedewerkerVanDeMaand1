package nl.weijzen.maastricht.medewerkervandemaand;

import android.content.Context;
import android.content.res.Resources;

import java.util.Calendar;

class CurrentYearAndMonth {
    private final Context context;
    private final Integer year;
    private final Integer month;

    // Constructors
    CurrentYearAndMonth(Context context){
        this.context = context;
        Calendar currentDate = Calendar.getInstance();
        this.year  = currentDate.get(Calendar.YEAR);
        this.month = currentDate.get(Calendar.MONTH);
    }

    // Getters
    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month + 1;
    }

    public String getMonthText() {
        Resources resources = this.context.getResources();
        String[] months     = resources.getStringArray(R.array.month_abbreviation);
        return months[this.month];
    }

    public String getMonthFullText() {
        Resources resources = this.context.getResources();
        String[] months     = resources.getStringArray(R.array.month_text);
        return months[this.month];
    }
}
