package uk.co.borconi.emil.obd2aa.gauge;


import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;

import uk.co.borconi.emil.obd2aa.ArcProgress;
import uk.co.borconi.emil.obd2aa.R;
import uk.co.borconi.emil.obd2aa.helpers.PreferencesHelper;

/**
 * Created by Emil on 01/09/2017.
 */

public class DrawGauges {

    public static Integer getLayoutForStyle(String layoutStyle) {
        switch (layoutStyle) {
            case "1":
                return R.layout.gauge_layout_1;
            case "2":
                return R.layout.gauge_layout_2;
            case "3":
                return R.layout.gauge_layout_3;
            default:
                throw new RuntimeException("Invalid layout");
        }
    }

    public static void renderAutoLayout(ConstraintLayout mywrapper, Context context, Display display) {
        PreferencesHelper prefs = PreferencesHelper.getPreferences(context);
        int numberOfGauges = prefs.getNumberOfGauges();
        int maxNumberOfGaugesInRow = prefs.getAutoLayoutNumberOfRowGauges();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getMetrics(displayMetrics);

        // constant - 16dp vertical gap
        int verticalGaugeGap = 16 * (displayMetrics.densityDpi / 160);

        int numberOfGaugesInRow = Math.min(numberOfGauges, maxNumberOfGaugesInRow);
        int numberOfRows = ((numberOfGauges - 1) / numberOfGaugesInRow) + 1;

        // +3 for extra spaces on top and bottom
        int desiredGaugeHeight = (displayMetrics.heightPixels - (verticalGaugeGap * (numberOfRows + 3))) / numberOfRows;

        int desiredHorizontalGap = (displayMetrics.widthPixels - (numberOfGaugesInRow * desiredGaugeHeight)) / (numberOfGaugesInRow + 1);

        int desiredGaugeWidth = (displayMetrics.widthPixels - (desiredHorizontalGap * (numberOfGaugesInRow + 1))) / numberOfGaugesInRow;

        int gaugeSize = Math.min(desiredGaugeWidth, desiredGaugeHeight);
        int gaugeGap = Math.max((displayMetrics.widthPixels - (numberOfGaugesInRow * gaugeSize)) / (numberOfGaugesInRow + 1), 50);

        int[] ids = new int[numberOfGauges];
        for (int gaugeIndex = 0; gaugeIndex < numberOfGauges; gaugeIndex++) {
            ids[gaugeIndex] = View.generateViewId();
        }
        for (int gaugeIndex = 0; gaugeIndex < numberOfGauges; gaugeIndex++) {
            ArcProgress newArc = setProperties(new ArcProgress(context), context, gaugeIndex + 1);
            newArc.setTag(String.format("gauge_%s", gaugeIndex + 1));
            newArc.setId(ids[gaugeIndex]);
            Log.d("OBD2", "Adding gauge: " + newArc);
            newArc.setLayoutParams(new ConstraintLayout.LayoutParams(gaugeSize, gaugeSize));
            mywrapper.addView(newArc);
        }
        Flow flow = mywrapper.findViewById(R.id.auto_grid);
        flow.setHorizontalGap(gaugeGap);
        flow.setReferencedIds(ids);
    }

    public static void renderSetLayout(ConstraintLayout mywrapper, Context context, Display display) {
        PreferencesHelper prefs = PreferencesHelper.getPreferences(context);
        int numberOfGauges = prefs.getNumberOfGauges();

        for (int gagugeNumber = 1; gagugeNumber <= numberOfGauges; gagugeNumber++) {
            ArcProgress newArc = mywrapper.findViewWithTag("gauge_" + gagugeNumber);
            Log.d("OBD", "Found Arc: " + newArc);
            setProperties(newArc, context, gagugeNumber);
        }
    }

    private static ArcProgress setProperties(ArcProgress newArc, Context context, int gaugeNumber) {
        if (newArc == null) {
            return null;
        }
        PreferencesHelper prefs = PreferencesHelper.getPreferences(context);
        newArc.setProgress(0);
        newArc.setBottomText(prefs.getNameForGauge(gaugeNumber));
        newArc.setisReverse(prefs.isReversedForGauge(gaugeNumber));
        newArc.setFinishedStrokeColor(prefs.getArchColor());
        newArc.setWarn1Color(prefs.getWarn1Color());
        newArc.setWarn2Color(prefs.getWarn2Color());
        newArc.setWarn1(prefs.getWarn1LevelForGauge(gaugeNumber));
        newArc.setWarn2(prefs.getWarn2LevelForGauge(gaugeNumber));
        newArc.setMax(prefs.getMaxValueForGauge(gaugeNumber));
        newArc.setMin(prefs.getMinValueForGauge(gaugeNumber));
        newArc.setGaugeStyle(prefs.getStyleForGauge(gaugeNumber));
        newArc.setSuffixText(prefs.getUnitForGauge(gaugeNumber));
        newArc.setTextColor(prefs.getTextColor());
        newArc.setStrokeWidth(prefs.getArchWidth());

        if (prefs.getStyleForGauge(gaugeNumber) == 6) {
            newArc.setBackgroundResource(R.drawable.bg1);
            newArc.setShowNeedle(false);
        } else if (prefs.getStyleForGauge(gaugeNumber) == 7) {
            newArc.setBackgroundResource(R.drawable.bg2);
            newArc.setShowNeedle(false);
        } else {
            newArc.setArcAngle(360 * 0.8f);
            newArc.setShowNeedle(prefs.shouldShowNeedleForGauge(gaugeNumber));
            newArc.setNeedleColor(prefs.getNeedleColor());
        }
        newArc.setShowArc(prefs.shouldShowScaleForGauge(gaugeNumber));
        newArc.setShowText(prefs.shouldShowTextForGauge(gaugeNumber));
        newArc.setUseGradientColor(prefs.shouldUseGradientTextForGauge(gaugeNumber));
        newArc.setShowDecimal(prefs.shouldShowDecimalForGauge(gaugeNumber));
        newArc.setShowUnit(prefs.shouldShowUnitForGauge(gaugeNumber));
        return newArc;
    }
}
