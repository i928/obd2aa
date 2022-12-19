package uk.co.borconi.emil.obd2aa;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by Emil on 12/08/2017.
 * <p>
 * Created by bruce on 11/6/14.
 */
public class ArcProgress extends View
{
    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_STROKE_WIDTH = "stroke_width";
    private static final String INSTANCE_SUFFIX_TEXT_SIZE = "suffix_text_size";
    private static final String INSTANCE_SUFFIX_TEXT_PADDING = "suffix_text_padding";
    private static final String INSTANCE_BOTTOM_TEXT_SIZE = "bottom_text_size";
    private static final String INSTANCE_BOTTOM_TEXT = "bottom_text";
    private static final String INSTANCE_TEXT_SIZE = "text_size";
    private static final String INSTANCE_TEXT_COLOR = "text_color";
    private static final String INSTANCE_PROGRESS = "progress";
    private static final String INSTANCE_MAX = "max";
    private static final String INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color";
    private static final String INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color";
    private static final String INSTANCE_ARC_ANGLE = "arc_angle";
    private static final String INSTANCE_SUFFIX = "suffix";
    private final RectF rectF = new RectF();
    private final int defaultLow1Color = Color.rgb(255, 0, 0);
    private final int defaultLow2Color = Color.rgb(255, 106, 0);
    private final int defaultUnfinishedColor = Color.argb(50, 224, 224, 224);
    private final int defaultTextColor = Color.rgb(66, 145, 241);
    private final float defaultSuffixTextSize;
    private final float defaultSuffixPadding;
    private final float defaultBottomTextSize;
    private final float defaultStrokeWidth;
    private final String defaultSuffixText;
    private final int minSize;
    protected Paint textPaint;
    //Typeface type = Typeface.createFromAsset(getContext().getAssets(), "fonts/RobotoCondensed.ttf");

    private Paint paint;
    private float strokeWidth;
    private float suffixTextSize;
    private float bottomTextSize;
    private String bottomText;
    private float textSize;
    private int textColor;
    private float progress = 0;
    private float max;
    private float min;
    private int finishedStrokeColor;
    private int unfinishedStrokeColor;
    private int low1Color;
    private int low2Color;
    private float warn1;
    private float warn2;
    private boolean isReverse;

    private boolean showTickMarks;
    private int numberOfMajorTicks;
    private int numberOfMinorSubTicks;
    ArrayList<float[]> tickPositions = new ArrayList<float[]>();
    int tickStartAngle = -145;
    int tickEndAngle = 145;

    private boolean showText;
    private boolean showNeedle;
    private boolean showDecimal;
    private boolean showUnit;
    private float arcAngle;
    private String suffixText = "%";
    private float suffixTextPadding;
    private Bitmap needleBitmap = null;
    private float arcBottomHeight;
    private float defaultTextSize;
    private int gaugeStyle;
    private boolean useGradientColor = false;
    private RectF oval = new RectF();
    private SweepGradient gradient;
    private int myRadius;
    private float cx;
    private int needleColor = -1;
    private int indent = 0;
    private float startPosition = 270;

    public ArcProgress(Context context) {
        this(context, null);
    }

    public ArcProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcProgress(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        defaultTextSize = Utils.sp2px(getResources(), 18);
        minSize = (int) Utils.dp2px(getResources(), 100);
        defaultTextSize = Utils.sp2px(getResources(), 35);
        defaultSuffixTextSize = Utils.sp2px(getResources(), 15);
        defaultSuffixPadding = Utils.dp2px(getResources(), 4);
        defaultSuffixText = "%";
        defaultBottomTextSize = Utils.sp2px(getResources(), 10);
        defaultStrokeWidth = Utils.dp2px(getResources(), 4);
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ArcProgress, defStyleAttr, 0);
        initByAttributes(attributes);
        attributes.recycle();
        initPainters();
    }

    protected void initByAttributes(TypedArray attributes)
    {
        finishedStrokeColor = attributes.getColor(R.styleable.ArcProgress_arc_finished_color, Color.WHITE);
        low1Color = attributes.getColor(R.styleable.ArcProgress_low1_color, defaultLow1Color);
        low2Color = attributes.getColor(R.styleable.ArcProgress_low2_color, defaultLow2Color);
        unfinishedStrokeColor = attributes.getColor(R.styleable.ArcProgress_arc_unfinished_color, defaultUnfinishedColor);
        textColor = attributes.getColor(R.styleable.ArcProgress_arc_text_color, defaultTextColor);
        textSize = attributes.getDimension(R.styleable.ArcProgress_arc_text_size, defaultTextSize);
        arcAngle = attributes.getFloat(R.styleable.ArcProgress_arc_angle, 360 * 0.73f);
        warn1 = attributes.getInt(R.styleable.ArcProgress_warn1, 75);
        warn2 = attributes.getInt(R.styleable.ArcProgress_warn2, 85);
        isReverse = attributes.getBoolean(R.styleable.ArcProgress_isReverse, false);

        showTickMarks = attributes.getBoolean(R.styleable.ArcProgress_showTickMarks, true);
        numberOfMajorTicks = attributes.getInt(R.styleable.ArcProgress_numberOfMajorTicks, 10);
        numberOfMinorSubTicks = attributes.getInt(R.styleable.ArcProgress_numberOfMinorSubTicks, 0);

        showText = attributes.getBoolean(R.styleable.ArcProgress_showtext, false);
        showNeedle = attributes.getBoolean(R.styleable.ArcProgress_showNeedle, true);
        showDecimal = attributes.getBoolean(R.styleable.ArcProgress_showNeedle, false);
        showUnit = attributes.getBoolean(R.styleable.ArcProgress_showNeedle, false);
        setMax(attributes.getInt(R.styleable.ArcProgress_arc_max, 100));
        setMin(attributes.getInt(R.styleable.ArcProgress_arc_min, 0));
        setGaugeStyle(attributes.getInt(R.styleable.ArcProgress_gaugestyle, 0));
        setProgress(attributes.getInt(R.styleable.ArcProgress_arc_progress, 0));
        strokeWidth = attributes.getDimension(R.styleable.ArcProgress_arc_stroke_width, defaultStrokeWidth);
        suffixTextSize = attributes.getDimension(R.styleable.ArcProgress_arc_suffix_text_size, defaultSuffixTextSize);
        suffixText = TextUtils.isEmpty(attributes.getString(R.styleable.ArcProgress_arc_suffix_text)) ? defaultSuffixText : attributes.getString(R.styleable.ArcProgress_arc_suffix_text);
        suffixTextPadding = attributes.getDimension(R.styleable.ArcProgress_arc_suffix_text_padding, defaultSuffixPadding);
        bottomTextSize = attributes.getDimension(R.styleable.ArcProgress_arc_bottom_text_size, defaultBottomTextSize);
        bottomText = attributes.getString(R.styleable.ArcProgress_arc_bottom_text) == null ? "" : attributes.getString(R.styleable.ArcProgress_arc_bottom_text);
    }

    protected void initPainters()
    {
        textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        textPaint.setAntiAlias(true);

        paint = new Paint();
        paint.setColor(defaultUnfinishedColor);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void invalidate()
    {
        initPainters();
        super.invalidate();
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth)
    {
        this.strokeWidth = strokeWidth;
        this.invalidate();
    }

    public void setNeedleColor(int color) {
        needleColor = color;
    }

    public void setIndent(int indent)
    {
        this.indent = indent;
        this.invalidate();
    }

    public void setStartPosition(int startPosition)
    {
        this.startPosition = startPosition;
        this.invalidate();
    }

    public void setShowTickMarks(boolean showTickMarks)
    {
        this.showTickMarks = showTickMarks;
        this.invalidate();
    }

    public void setNumberOfMajorTicks(int numberOfMajorTicks)
    {
        this.numberOfMajorTicks = numberOfMajorTicks;
        this.invalidate();
    }

    public void setNumberOfMinorTicks(int numberOfMinorSubTicks)
    {
        this.numberOfMinorSubTicks = numberOfMinorSubTicks;
        this.invalidate();
    }

    public void setShowText(boolean setShowText)
    {
        this.showText = setShowText;
        this.invalidate();
    }

    public void setNeedleBitmap(Bitmap needle)
    {
        this.needleBitmap = needle;
        this.invalidate();
    }

    public void setShowNeedle(boolean setShowNeedle)
    {
        this.showNeedle = setShowNeedle;
        this.invalidate();
    }

    public void setShowUnit(boolean setShowUnit)
    {
        this.showUnit = setShowUnit;
        this.invalidate();
    }

    public void setShowDecimal(boolean setShowDecimal)
    {
        this.showDecimal = setShowDecimal;
        this.invalidate();
    }

    public void setisReverse(boolean setisReverse)
    {
        this.isReverse = setisReverse;
        this.invalidate();
    }

    public void setWarn1(float warn1)
    {
        this.warn1 = warn1;
        this.invalidate();
    }

    public void setWarn2(float warn2)
    {
        this.warn2 = warn2;
        this.invalidate();
    }

    public void setWarn1Color(int unfinishedStrokeColor)
    {
        this.low1Color = unfinishedStrokeColor;
        this.invalidate();
    }

    public void setWarn2Color(int unfinishedStrokeColor)
    {
        this.low2Color = unfinishedStrokeColor;
        this.invalidate();
    }

    public float getSuffixTextSize() {
        return suffixTextSize;
    }

    public void setSuffixTextSize(float suffixTextSize)
    {
        this.suffixTextSize = suffixTextSize;
        this.invalidate();
    }

    public String getBottomText() {
        return bottomText;
    }

    public void setBottomText(String bottomText)
    {
        this.bottomText = bottomText;
        this.invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress)
    {
        this.progress = progress;
        if (this.progress > getMax()) {
            this.progress %= getMax();
        }
        invalidate();
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max)
    {
        this.max = max;
        invalidate();
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min)
    {
        this.min = min;
        invalidate();
    }

    public int getGaugeStyle() {
        return gaugeStyle;
    }

    public void setGaugeStyle(int gaugestyle)
    {
        this.gaugeStyle = gaugestyle;
        invalidate();
    }

    public float getBottomTextSize() {
        return bottomTextSize;
    }

    public void setBottomTextSize(float bottomTextSize)
    {
        this.bottomTextSize = bottomTextSize;
        this.invalidate();
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize)
    {
        this.textSize = textSize;
        this.invalidate();
    }

    public boolean getUseGradientColor() {
        return this.useGradientColor;
    }

    public void setUseGradientColor(boolean useGradientColor)
    {
        this.useGradientColor = useGradientColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor)
    {
        this.textColor = textColor;
        this.invalidate();
    }

    public int getFinishedStrokeColor() {
        return finishedStrokeColor;
    }

    public void setFinishedStrokeColor(int finishedStrokeColor)
    {
        this.finishedStrokeColor = finishedStrokeColor;
        this.invalidate();
    }

    public int getUnfinishedStrokeColor() {
        return unfinishedStrokeColor;
    }

    public void setUnfinishedStrokeColor(int unfinishedStrokeColor)
    {
        this.unfinishedStrokeColor = unfinishedStrokeColor;
        this.invalidate();
    }

    public float getArcAngle() {
        return arcAngle;
    }

    public void setArcAngle(float arcAngle)
    {
        this.arcAngle = arcAngle;
        this.invalidate();
    }

    public String getSuffixText() {
        return suffixText;
    }

    public void setSuffixText(String suffixText)
    {
        this.suffixText = suffixText;
        this.invalidate();
    }

    public float getSuffixTextPadding() {
        return suffixTextPadding;
    }

    public void setSuffixTextPadding(float suffixTextPadding)
    {
        this.suffixTextPadding = suffixTextPadding;
        this.invalidate();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return minSize;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return minSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int size = Math.min(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.getSize(widthMeasureSpec));
        setMeasuredDimension(size, size);
        if (needleBitmap != null)
        {
            Bitmap resized = resizeBitmap(needleBitmap, size, size);
            setNeedleBitmap(resized);
        }
        rectF.set(strokeWidth / 2f + indent, strokeWidth / 2f + indent, size - strokeWidth / 2f - indent, size - strokeWidth / 2f - indent);
        float radius = size / 2f;
        float angle = (360 - arcAngle) / 2f;
        arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
        setBottomTextSize((float) (size / 9.33));
        if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1 || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
        {
            setTextSize((float) (size / 6.1));
        }
        else
        {
            setTextSize((float) (size / 5.1));
        }
        setSuffixTextSize((float) (size / 10.25));

        if (size == 0)
            return;

        int[] colors = {finishedStrokeColor, low1Color, low2Color, low2Color, Color.BLACK};
        if (isReverse)
        {
            colors[0] = low2Color;
            colors[2] = finishedStrokeColor;
            colors[3] = finishedStrokeColor;
        }
        float[] positions = {0, (float) (warn1 * 0.8 / (getMax() - getMin())), (float) (warn2 * 0.8 / (getMax() - getMin())), 0.8f, 1};
        // Log.d("OBD2AA","Min: " + getMin() + "Max: " + getMax()+ " Arch points: "+0+ ", "+ (float) (warn1 * 0.8 / (getMax() - getMin()))+ ", "+ (float) (warn2 * 0.8 / (getMax() - getMin()))+ ", "+ 0.8f+ ", "+ 1);

        Matrix gradientMatrix = new Matrix();
        gradientMatrix.preRotate((startPosition - arcAngle / 2f) - 7, size / 2f, size / 2f);
        gradient = new SweepGradient(size / 2, size / 2, colors, positions);
        gradient.setLocalMatrix(gradientMatrix);

        if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1 || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
        {
            setStrokeWidth(30 / (480 / (float) size));
            float dist = (57 / (480 / (float) size));
            oval = new RectF(dist, dist, size - dist, size - dist);
        }

        myRadius = size / 2;
        cx = size / 2f;
        if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1 || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
        {
            float scaleLength = (14 / (480 / (float) size));
            for (int i = -135; i <= 135; i += 27)
            {

                float angle2 = (float) Math.toRadians(i); // Need to convert to radians first

                float startX = (float) (cx + (myRadius) * Math.sin(angle2));
                float startY = (float) (cx - (myRadius) * Math.cos(angle2));

                float stopX = (float) (cx + (myRadius - scaleLength) * Math.sin(angle2));
                float stopY = (float) (cx - (myRadius - scaleLength) * Math.cos(angle2));
                tickPositions.add(new float[]{startX, startY, stopX, stopY});
            }
        }
        else
        {
            float scaleMarkSize = getResources().getDisplayMetrics().density * getStrokeWidth();
            float majorTicksInterval = (this.tickEndAngle - this.tickStartAngle) / this.numberOfMajorTicks;
            float minorTicksInterval = this.numberOfMinorSubTicks != 0
                    ? majorTicksInterval / (this.numberOfMinorSubTicks + 1)
                    : 0;

            if (this.numberOfMajorTicks > 0)
            {
                for (float i = this.tickStartAngle; i <= this.tickEndAngle; i += majorTicksInterval)
                {
                    float angle2 = (float) Math.toRadians(i); // Need to convert to radians first

                    float startX = (float) (cx + (myRadius - 2 - getStrokeWidth()) * Math.sin(angle2));
                    float startY = (float) (cx - (myRadius - 2 - getStrokeWidth()) * Math.cos(angle2));

                    float stopX = (float) (cx + (myRadius - 2 - getStrokeWidth() - scaleMarkSize) * Math.sin(angle2));
                    float stopY = (float) (cx - (myRadius - 2 - getStrokeWidth() - scaleMarkSize) * Math.cos(angle2));
                    tickPositions.add(new float[]{startX, startY, stopX, stopY});
                    if (tickPositions.size() < this.numberOfMajorTicks * (numberOfMinorSubTicks+1) + 1
                            && this.numberOfMinorSubTicks > 0)
                    {
                        // only draw minor ticks if we did not draw the last major tick already
                        for (float j = i + minorTicksInterval; j <= i + (this.numberOfMinorSubTicks * minorTicksInterval); j += minorTicksInterval)
                        {
                            float subAngle2 = (float) Math.toRadians(j);

                            float subStartX = (float) (cx + (myRadius - 2 - getStrokeWidth()) * Math.sin(subAngle2));
                            float subStartY = (float) (cx - (myRadius - 2 - getStrokeWidth()) * Math.cos(subAngle2));

                            float subStopX = (float) (cx + (myRadius - 2 - getStrokeWidth() - (scaleMarkSize / 3)) * Math.sin(subAngle2));
                            float subStopY = (float) (cx - (myRadius - 2 - getStrokeWidth() - (scaleMarkSize / 3)) * Math.cos(subAngle2));

                            tickPositions.add(new float[]{subStartX, subStartY, subStopX, subStopY});
                        }
                    }
                }
            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        float startAngle = startPosition - arcAngle / 2f;
        float currentProgress = progress + Math.abs(getMin());
        float finishedSweepAngle = (currentProgress / (getMax() + Math.abs(getMin()))) * arcAngle;
        //Log.d("OBD2AA","Progress: "+progress + ", Curr_progress: "+currentProgress+ ", Max: "+ getMax()+", Min: "+getMin()+ ", arcAngle: "+arcAngle + ",Finished: "+finishedSweepAngle);
        //if(currentProgress == 0) finishedStartAngle = 0.01f;
        paint.setColor(unfinishedStrokeColor);
        //We need to calculate the store width, assuming

        paint.setStrokeWidth(strokeWidth);
        switch (getGaugeStyle())
        {
            case Constants.GaugeStyles.Clear:
            case Constants.GaugeStyles.BackgroundNoArch:
                break;
            case Constants.GaugeStyles.NoBackgroundPercentageArch:
                paint.setColor(unfinishedStrokeColor);
                paint.setShader(null);
                canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
                break;
            case Constants.GaugeStyles.NoBackgroundFullArch:
                paint.setColor(finishedStrokeColor);
                paint.setShader(gradient);
                canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
                paint.setShader(null);
                break;
            case Constants.GaugeStyles.BackgroundPercentageArch:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(unfinishedStrokeColor);
                paint.setShader(null);
                canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
                paint.setStyle(Paint.Style.STROKE);
                break;
            case Constants.GaugeStyles.BackgroundFullArch:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(finishedStrokeColor);
                paint.setShader(gradient);
                canvas.drawArc(rectF, startAngle, arcAngle, false, paint);
                paint.setShader(null);
                break;
        }

        if (getGaugeStyle() == Constants.GaugeStyles.NoBackgroundPercentageArch
                || getGaugeStyle() == Constants.GaugeStyles.BackgroundPercentageArch)
        {
            //paint.setStrokeCap(Paint.Cap.SQUARE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setShader(gradient);
            canvas.drawArc(rectF, startAngle, finishedSweepAngle, false, paint);
            //Log.d("OBD2AA","From angle: " + finishedStartAngle + "to angle: "+finishedSweepAngle);
            paint.setShader(null);
        }
        else if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1
                || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
        {
            //paint.setStrokeCap(Paint.Cap.SQUARE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setShader(gradient);
            canvas.drawArc(oval, startAngle, finishedSweepAngle, false, paint);
            //Log.d("OBD2AA","From angle: " + finishedStartAngle + "to angle: "+finishedSweepAngle);
            paint.setShader(null);
        }

        if (showTickMarks)
        {
            paint.setStyle(Paint.Style.STROKE);
            if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1
                    || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
            {
                paint.setColor(getFinishedStrokeColor());
            }
            else
            {
                paint.setColor(getFinishedStrokeColor());
            }
            paint.setStrokeWidth(2);
            if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1
                    || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
            {
                paint.setTextSize(getWidth() / 14);
                for (int i = 0; i < tickPositions.size(); i++)
                {
                    canvas.drawLine(tickPositions.get(i)[0], tickPositions.get(i)[1], tickPositions.get(i)[2], tickPositions.get(i)[3], paint);
                }
            }
            else
            {
                for (int i = 0; i < tickPositions.size(); i++)
                {
                    canvas.drawLine(tickPositions.get(i)[0], tickPositions.get(i)[1], tickPositions.get(i)[2], tickPositions.get(i)[3], paint);
                }
            }
            paint.setStrokeWidth(getStrokeWidth());
        }

        if (showNeedle)
        {
            if (needleBitmap == null)
            {
                paint.setShader(null);
                paint.setColor(needleColor);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(3);
                Path path = new Path();
                path.moveTo(cx, cx);
                path.lineTo(cx - 4, cx);
                path.lineTo(cx, cx + 4);
                path.lineTo(cx + (myRadius - 20 - indent), cx);
                path.lineTo(cx, cx - 4);
                path.lineTo(cx - 4, cx);
                path.close();
                Matrix needlematrix = new Matrix();
                needlematrix.preRotate(startAngle + finishedSweepAngle, cx, cx);
                path.transform(needlematrix);
                canvas.drawPath(path, paint);
                paint.setColor(Color.BLACK);
                canvas.drawCircle(cx, cx, 2, paint);
                paint.setStyle(Paint.Style.STROKE);
            }
            else
            {
                Matrix needlematrix = new Matrix();
                needlematrix.setRotate(startAngle + finishedSweepAngle, cx, cx);
                paint.setAntiAlias(true);
                paint.setAlpha(255);
                paint.setFilterBitmap(true);
                canvas.drawBitmap(needleBitmap, needlematrix, paint);
            }
        }

        String text = showDecimal ? String.format("%.2f", getProgress()) : String.format("%.0f", getProgress());
        float bottomTextBaseline = 0;
        float bottomTextHeigh = 0;

        if (arcBottomHeight == 0)
        {
            float radius = getWidth() / 2f;
            float angle = (360 - arcAngle) / 2f;
            arcBottomHeight = radius * (float) (1 - Math.cos(angle / 180 * Math.PI));
        }
        if (!TextUtils.isEmpty(getBottomText()))
        {
            if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1
                    || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
            {
                textPaint.setTextSize(bottomTextSize);
                bottomTextBaseline = getHeight() / 2 + (70 / (480 / (float) getWidth()));
                canvas.drawText(suffixText, (getWidth() - textPaint.measureText(suffixText)) / 2.0f, bottomTextBaseline, textPaint);
            }
            else
            {
                textPaint.setTextSize(bottomTextSize);
                canvas.drawText(getBottomText(), (getWidth() - textPaint.measureText(getBottomText())) / 2.0f, getHeight(), textPaint);
            }
        }

        if (!TextUtils.isEmpty(text))
        {
            textPaint.setColor(textColor);
            if (getUseGradientColor())
            {
                if (isReverse)
                {
                    if (currentProgress > warn2)
                    {
                        textPaint.setColor(getFinishedStrokeColor());
                    }
                    else if (currentProgress > warn1)
                    {
                        textPaint.setColor((low1Color));
                    }
                    else
                    {
                        textPaint.setColor(low2Color);
                    }
                }
                else
                {
                    if (currentProgress < warn1)
                    {
                        textPaint.setColor(getFinishedStrokeColor());
                    }
                    else if (currentProgress < warn2)
                    {
                        textPaint.setColor((low1Color));
                    }
                    else
                    {
                        textPaint.setColor(low2Color);
                    }
                }
            }
            textPaint.setTextSize(textSize);

            float textHeight = textPaint.descent() + textPaint.ascent();
            float textBaseline = (getHeight() - textHeight) / 2.0f;

            if (showText)
            {
                if (!showNeedle)
                {
                    canvas.drawText(text, (getWidth() - textPaint.measureText(text)) / 2.0f, textBaseline, textPaint);
                }
                else
                {
                    Rect textBounds = new Rect();
                    textPaint.getTextBounds(getBottomText(), 0, getBottomText().length(), textBounds);
                    textPaint.setTextSize((float) (getBottomTextSize() * 1.8));
                    bottomTextHeigh = bottomTextBaseline + textBounds.top;
                    canvas.drawText(text, (getWidth() - textPaint.measureText(text)) / 2.0f, getHeight() + bottomTextHeigh, textPaint);
                }
            }
            textPaint.setTextSize(suffixTextSize);
            float suffixHeight = textPaint.descent() + textPaint.ascent();

            if (showUnit)
            {
                if (!showNeedle)
                {
                    if (getGaugeStyle() == Constants.GaugeStyles.PartialFilled1
                            || getGaugeStyle() == Constants.GaugeStyles.PartialFilled2)
                    {
                        textHeight = getHeight() / 2 - (40 / (480 / (float) getWidth()));
                        textPaint.setColor(getTextColor());
                        canvas.drawText(getBottomText(), (getWidth() - textPaint.measureText(getBottomText())) / 2.0f, textHeight, textPaint);
                    }
                    else
                    {
                        textBaseline = (getHeight() - textHeight) / 2.0f;
                        canvas.drawText(suffixText, getWidth() / 2.0f + textPaint.measureText(text) + suffixTextPadding + 5, textBaseline + textHeight - suffixHeight, textPaint);
                    }
                }
                else
                {
                    textPaint.setTextSize(textSize);
                    canvas.drawText(suffixText, (getWidth() - textPaint.measureText(suffixText)) / 2.0f, textBaseline + textHeight + 5, textPaint);
                }
            }
        }
    }

    public Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight)
    {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_SIZE, getSuffixTextSize());
        bundle.putFloat(INSTANCE_SUFFIX_TEXT_PADDING, getSuffixTextPadding());
        bundle.putFloat(INSTANCE_BOTTOM_TEXT_SIZE, getBottomTextSize());
        bundle.putString(INSTANCE_BOTTOM_TEXT, getBottomText());
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize());
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor());
        bundle.putFloat(INSTANCE_PROGRESS, getProgress());
        bundle.putFloat(INSTANCE_MAX, getMax());
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedStrokeColor());
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedStrokeColor());
        bundle.putFloat(INSTANCE_ARC_ANGLE, getArcAngle());
        bundle.putString(INSTANCE_SUFFIX, getSuffixText());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        if (state instanceof Bundle)
        {
            final Bundle bundle = (Bundle) state;
            strokeWidth = bundle.getFloat(INSTANCE_STROKE_WIDTH);
            suffixTextSize = bundle.getFloat(INSTANCE_SUFFIX_TEXT_SIZE);
            suffixTextPadding = bundle.getFloat(INSTANCE_SUFFIX_TEXT_PADDING);
            bottomTextSize = bundle.getFloat(INSTANCE_BOTTOM_TEXT_SIZE);
            bottomText = bundle.getString(INSTANCE_BOTTOM_TEXT);
            textSize = bundle.getFloat(INSTANCE_TEXT_SIZE);
            textColor = bundle.getInt(INSTANCE_TEXT_COLOR);
            setMax(bundle.getFloat(INSTANCE_MAX));
            setProgress(bundle.getFloat(INSTANCE_PROGRESS));
            finishedStrokeColor = bundle.getInt(INSTANCE_FINISHED_STROKE_COLOR);
            unfinishedStrokeColor = bundle.getInt(INSTANCE_UNFINISHED_STROKE_COLOR);
            suffixText = bundle.getString(INSTANCE_SUFFIX);
            initPainters();
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }
}