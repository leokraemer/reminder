package de.leo.fingerprint.datacollector.ComboSeekBar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import java.util.List;

/**
 * seekbar background with text on it.
 * 
 * @author sazonov-adm
 * 
 */
public class CustomDrawable extends Drawable {
    public final String t = "CustomDrawable";
	private final ComboSeekBar mySlider;
	private final Drawable myBase;
	private final Paint textUnselected;
	private float mThumbRadius;
	/**
	 * paints.
	 */
	private final Paint unselectLinePaint;
	private List<ComboSeekBar.Dot> mDots;
	private Paint selectLinePaint;
	private Paint circleLinePaint;
	private float mDotRadius;
	private Paint textSelected;
	private int mTextSize;
	private float mTextMargin;
	private int mTextHeight;
	private boolean mIsMultiline;
    private int textLines = 1; //SA
    public static float yLine = 0;//SA
    private float mPadding;

	public CustomDrawable(Drawable base, ComboSeekBar slider, float thumbRadius, List<ComboSeekBar.Dot> dots, int color, int textSize, boolean isMultiline, float padding) {
		mIsMultiline = isMultiline;
		mySlider = slider;
		myBase = base;
		mDots = dots;
		mTextSize = textSize;
		textUnselected = new Paint(Paint.ANTI_ALIAS_FLAG);
		textUnselected.setColor(color);
		textUnselected.setAlpha(255);

		textSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
		textSelected.setTypeface(Typeface.DEFAULT_BOLD);
		textSelected.setColor(color);
		textSelected.setAlpha(255);

		mThumbRadius = thumbRadius;

		unselectLinePaint = new Paint();
		unselectLinePaint.setColor(color);
		unselectLinePaint.setStrokeWidth(toPix(3));

		selectLinePaint = new Paint();
		selectLinePaint.setColor(color);
		selectLinePaint.setStrokeWidth(toPix(3));

		circleLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		circleLinePaint.setColor(color);

		Rect textBounds = new Rect();
		textSelected.setTextSize((int) (mTextSize));
		textSelected.setColor(color);
		textSelected.getTextBounds("M", 0, 1, textBounds);

		textUnselected.setTextSize(mTextSize);
		textUnselected.setColor(color);
		textSelected.setTextSize(mTextSize);

		mTextHeight = textBounds.height();
		mDotRadius = toPix(5);
		mTextMargin = toPix(10);
        mPadding = padding;

        //for set bounds
        int tl = 0;
        textLines = 1;
        for (ComboSeekBar.Dot dot: mDots){
            tl= myTrim(dot.text.toString()).split("\\|").length;
            if(tl>textLines)
                textLines = tl;
        }
        //Log.d(t,"textLines----->"+textLines);
        setBounds(0,0,getBounds().width(),(int) (mThumbRadius + mTextMargin + mTextHeight * textLines + mPadding*2));
	}

	private float toPix(int size) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, mySlider.getContext().getResources().getDisplayMetrics());
	}

	@Override
	protected final void onBoundsChange(Rect bounds) {
		myBase.setBounds(bounds);
	}

	@Override
	protected final boolean onStateChange(int[] state) {
		invalidateSelf();
		return false;
	}

	@Override
	public final boolean isStateful() {
		return true;
	}

	@Override
	public final void draw(Canvas canvas) {
		// Log.d("--- draw:" + (getBounds().right - getBounds().left));
        //get text height

		//int height = this.getIntrinsicHeight() / 2;
        int height = (int)mPadding;
		if (mDots.size() == 0) {
			canvas.drawLine(0, height, getBounds().right, height, unselectLinePaint);
			return;
		}
       // int heightLine = height + (textLines-1) * mTextHeight;
       // Log.d(t,"heightLine------>" + heightLine);
        //Log.d(t,"height------>" + height);
        yLine = 0;
		for (ComboSeekBar.Dot dot : mDots) {
			drawText(canvas, dot, dot.mX, height);
		}
        //Log.d(t,"getBounds().height()------->"+ getBounds().height());
        //Log.d(t,"yLine---------->"+ yLine);


        //draw line
        canvas.drawLine(mDots.get(0).mX, yLine, mDots.get(mDots.size() - 1).mX, yLine, selectLinePaint);
        for (ComboSeekBar.Dot dot : mDots) {
          /*  if (dot.isSelected) {
                canvas.drawLine(mDots.get(0).mX, yLine, dot.mX, yLine, selectLinePaint);
                canvas.drawLine(dot.mX, yLine, mDots.get(mDots.size() - 1).mX, yLine, unselectLinePaint);
            }*/
            canvas.drawCircle(dot.mX, yLine, mDotRadius, circleLinePaint);
        }
	}

	/**
	 * @param canvas
	 *            canvas.
	 * @param dot
	 *            current dot.
	 * @param x
	 *            x cor.
	 * @param y
	 *            y cor.
	 */
	private void drawText(Canvas canvas, ComboSeekBar.Dot dot, float x, float y) {
		final Rect textBounds = new Rect();
        int lastLength = 0;
        String lastWord = null;
        //Log.d(t,"dot.text.toString()---------->" + dot.text.toString());
        //Log.d(t,"dot.text.toCharArray()---------->" + dot.text.toCharArray());
        if(dot.text != null){
            for (String line : myTrim(dot.text.toString()).split("\\|")) {
                //Log.d(t,"lastWord line---------->" + line);
                if(line.length() > lastLength) {
                    //Log.d(t,"line.length" + line.length());
                    lastLength = line.length();
                    lastWord = line;
                }
            }
            //NullPointerException. do not know why
            if(lastWord != null) {
                textSelected.getTextBounds(lastWord, 0, lastWord.length(), textBounds);
            }else{
                textSelected.getTextBounds(dot.text, 0, dot.text.length(), textBounds);
            }
        }else {
            textSelected.getTextBounds(dot.text, 0, dot.text.length(), textBounds);
        }

		float xres;
        //Log.d(t,"getBounds().width()------->"+getBounds().width());
        //Log.d(t,"textBounds.width()------->"+textBounds.width());
		if (dot.id == (mDots.size() - 1)) {
			xres = getBounds().width() - textBounds.width();
		} else if (dot.id == 0) {
			xres = 0;
		} else {
			xres = x - (textBounds.width() / 2);
		}
        //Log.d(t,"xres------->"+xres);
		float yres;
/*		if (mIsMultiline) {
			if ((dot.id % 2) == 0) {
				yres = y - mTextMargin - mDotRadius;
			} else {
				yres = y + mTextHeight;
			}
		} else {
			yres = y - (mDotRadius * 2) + mTextMargin;
		}*/
        yres = y+mTextMargin;

       // Log.d(t, "yres------->" + yres);
/*		if (dot.isSelected) {
			canvas.drawText(dot.text, xres, yres, textSelected);
		} else {
			canvas.drawText(dot.text, xres, yres, textUnselected);
		}*/

        //SA:3/8/2015

        if( dot.text != null) {
            String s = dot.text.toString();
            for (String line : myTrim(s).split("\\|")) {
                // Log.d(t,"dot.text.trim()"+s.trim());
                // Log.d(t,"line------->"+line);
                canvas.drawText(line, xres, yres, textUnselected);
                yres += textUnselected.descent() - textUnselected.ascent();
            }
            if (yLine < yres) {
                yLine = yres;
            }
        }
	}

	@Override
	public final int getIntrinsicHeight() {
		if (mIsMultiline) {
			return (int) (selectLinePaint.getStrokeWidth() + mDotRadius + (mTextHeight * textLines) * 2  + mTextMargin + mPadding*2);
		} else {
			//return (int) (mThumbRadius + mTextMargin + mTextHeight + mDotRadius);
            return (int) (mThumbRadius + mTextMargin + mTextHeight * textLines + mPadding*2);
		}
	}

	@Override
	public final int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha(int alpha) {
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
	}

    //space from cvs is not normal space
    public String myTrim(String s){
        String result = null;
        result = s.replaceAll("\\s+", "");
        result = result.replaceAll(String.valueOf((char) 160), " ");
        return result.trim();
    }
}