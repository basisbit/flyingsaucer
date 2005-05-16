/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbj�rn Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.render;

import org.xhtmlrenderer.css.Border;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.layout.Context;
import org.xhtmlrenderer.layout.FontUtil;
import org.xhtmlrenderer.layout.block.Relative;
import org.xhtmlrenderer.layout.content.StylePush;
import org.xhtmlrenderer.layout.inline.VerticalAlign;
import org.xhtmlrenderer.util.GraphicsUtil;

import java.awt.*;
import java.awt.font.LineMetrics;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * Description of the Class
 *
 * @author Joshua Marinacci
 * @author Torbj�rn Gannholm
 */
public class InlineRendering {

    /**
     * Description of the Method
     *
     * @param c      PARAM
     * @param inline PARAM
     * @param lx     PARAM
     * @param ly     PARAM
     * @param lm
     */
    public static void paintSelection(Context c, InlineBox inline, int lx, int ly, LineMetrics lm) {
        if (c.inSelection(inline)) {
            int dw = inline.width - 2;
            int xoff = 0;
            if (c.getSelectionEnd() == inline) {
                dw = c.getSelectionEndX();
            }
            if (c.getSelectionStart() == inline) {
                xoff = c.getSelectionStartX();
            }
            c.getGraphics().setColor(new Color(200, 200, 255));
            ((Graphics2D) c.getGraphics()).setPaint(new GradientPaint(0, 0, new Color(235, 235, 255),
                    0, inline.height / 2, new Color(190, 190, 235),
                    true));
            int top = ly + inline.y - (int) Math.ceil(lm.getAscent());
            int height = (int) Math.ceil(lm.getAscent() + lm.getDescent());
            c.getGraphics().fillRect(lx + inline.x + xoff,
                    top,
                    dw - xoff,
                    height);
        }
    }


    /**
     * Description of the Method
     *
     * @param c      PARAM
     * @param ix     PARAM
     * @param iy     PARAM
     * @param inline PARAM
     * @param lm
     */
    public static void paintText(Context c, int ix, int iy, InlineTextBox inline, LineMetrics lm) {
        String text = inline.getSubstring();
        Graphics g = c.getGraphics();
        //adjust font for current settings
        Font oldfont = c.getGraphics().getFont();
        c.getGraphics().setFont(FontUtil.getFont(c));
        Color oldcolor = c.getGraphics().getColor();
        c.getGraphics().setColor(c.getCurrentStyle().getColor());

        //baseline is baseline! iy -= (int) lm.getDescent();

        //draw the line
        if (text != null && text.length() > 0) {
            c.getTextRenderer().drawString(c.getGraphics(), text, ix, iy);
        }

        c.getGraphics().setColor(oldcolor);
        if (c.debugDrawFontMetrics()) {
            g.setColor(Color.red);
            g.drawLine(ix, iy, ix + inline.width, iy);
            iy += (int) Math.ceil(lm.getDescent());
            g.drawLine(ix, iy, ix + inline.width, iy);
            iy -= (int) Math.ceil(lm.getDescent());
            iy -= (int) Math.ceil(lm.getAscent());
            g.drawLine(ix, iy, ix + inline.width, iy);
        }

        // restore the old font
        c.getGraphics().setFont(oldfont);
    }

    /**
     * @param c      PARAM
     * @param line   PARAM
     * @param inline PARAM
     * @param lm
     */
    public static void paintBackground(Context c, LineBox line, InlineBox inline, int padX, LineMetrics lm) {
        //Uu.p("painting border: " + inline.border);
        // paint the background
        //int padding_xoff = inline.totalLeftPadding(c.getCurrentStyle());
        CalculatedStyle style = c.getCurrentStyle();
        int parent_width = line.getParent().width;
        Border border = style.getBorderWidth(c.getCtx());
        //note: percentages here refer to width of containing block
        Border margin = style.getMarginWidth(parent_width, parent_width, c.getCtx());
        Border padding = style.getPaddingWidth(parent_width, parent_width, c.getCtx());
        int padding_yoff = margin.top + border.top + padding.top;

        int ty = line.getBaseline() - inline.y - inline.height - padding_yoff + line.y;

        ty += (int) lm.getDescent();
        // adjust because padding isn't used for position on inlines
        // JMM: This isn't correct!!
        //c.translate(-padding_xoff, ty);
        // just doing adjustments for vertical insets now
        c.translate(0, ty);
        //int old_width = inline.width;
        //int old_height = inline.height;
        //inline.width = inline.width - padX - inline.rightPadding;
        //inline.height += margin.top + border.top + padding.top + padding.bottom + border.bottom + margin.bottom;
        // paint the actual background
        //BoxRendering.paintBackground(c, inline);
        //Border margin = c.getCurrentStyle().getMarginWidth(c.getBlockFormattingContext().getWidth(), c.getBlockFormattingContext().getHeight());
        Rectangle box = new Rectangle(inline.x,
                inline.y + margin.top + border.top,
                inline.width - inline.leftPadding - inline.rightPadding,
                inline.height + padding.top + padding.bottom);

        // paint the background
        Color background_color = BoxRendering.getBackgroundColor(c);
        if (background_color != null) {
            // skip transparent background
            if (!background_color.equals(BackgroundPainter.transparent)) {
                //TODO. make conf controlled Uu.p("filling a background");
                c.getGraphics().setColor(background_color);
                c.getGraphics().fillRect(box.x, box.y, box.width, box.height);
            }
        }

        /*Rectangle bounds = new Rectangle(inline.x,
                inline.y + margin.top,
                inline.width + 1,
                inline.height - margin.top - margin.bottom);
        BorderPainter.paint(bounds, BorderPainter.TOP + BorderPainter.BOTTOM, c.getCurrentStyle(), c.getGraphics(), c.getCtx());
        */
        //inline.width = old_width;
        //inline.height = old_height;
        //c.translate(+padding_xoff, -ty);
        c.translate(0, -ty);

        for (Iterator i = c.getInlineBorders().iterator(); i.hasNext();) {
            ((InlineBorder) i.next()).paint(c,
                    line,
                    inline.x,
                    inline.width - inline.leftPadding - inline.rightPadding,
                    BorderPainter.TOP + BorderPainter.BOTTOM);
        }
    }

    /**
     * @param c       PARAM
     * @param line    PARAM
     * @param inline  PARAM
     * @param margin
     * @param border
     * @param padding
     */
    public static void paintLeftPadding(Context c, LineBox line, InlineBox inline, int padX, Border margin, Border border, Border padding) {
        int padding_yoff = margin.top + border.top + padding.top;
        //if (padding.left <= 0 && border.left <= 0) return;

        int ty = line.getBaseline() - inline.y - inline.height - padding_yoff + line.y;

        LineMetrics lm = FontUtil.getLineMetrics(c, inline);
        ty += (int) lm.getDescent();
        c.translate(padX, ty);
        //int old_width = inline.width;
        //int old_height = inline.height;
        //sets the inline borders to the current style, so border objects will exist
        //inline.height += margin.top + border.top + padding.top + padding.bottom + border.bottom + margin.bottom;
        //inline.width = margin.left + border.left + padding.left;

        if (padding.left > 0) {
            // paint the background
            Color background_color = BoxRendering.getBackgroundColor(c);
            if (background_color != null) {
                // skip transparent background
                if (!background_color.equals(BackgroundPainter.transparent)) {
                    Rectangle box = new Rectangle(inline.x + margin.left + border.left,
                            inline.y + margin.top + border.top,
                            padding.left + 1,
                            inline.height + padding.top + padding.bottom + 1);
                    //TODO. make conf controlled Uu.p("filling a background");
                    c.getGraphics().setColor(background_color);
                    c.getGraphics().fillRect(box.x, box.y, box.width, box.height);
                }
            }
        }

        /*if (border.left > 0) {
            Rectangle bounds = new Rectangle(inline.x + padX + margin.left,
                    inline.y + margin.top,
                    inline.width - margin.left,
                    inline.height - margin.top - margin.bottom);
            BorderPainter.paint(bounds, BorderPainter.LEFT + BorderPainter.TOP + BorderPainter.BOTTOM, c.getCurrentStyle(), c.getGraphics(), c.getCtx());
        }*/
        /*inline.width = old_width;
        inline.height = old_height;*/
        //c.translate(+padding_xoff, -ty);
        c.translate(-padX, -ty);

        for (Iterator i = c.getInlineBorders().iterator(); i.hasNext();) {
            ((InlineBorder) i.next()).paint(c,
                    line,
                    padX + inline.x + margin.left,
                    padding.left + border.left,
                    BorderPainter.TOP + BorderPainter.BOTTOM);
        }

        InlineBorder ib = new InlineBorder(inline.y, inline.height, margin, border, padding, c.getCurrentStyle(), lm);
        ib.paint(c, line,
                inline.x + padX + margin.left,
                border.left + padding.left, BorderPainter.LEFT + BorderPainter.TOP + BorderPainter.BOTTOM);
        c.getInlineBorders().addLast(ib);
    }

    /**
     * @param c       PARAM
     * @param line    PARAM
     * @param inline  PARAM
     * @param margin
     * @param border
     * @param padding
     */
    public static void paintRightPadding(Context c, LineBox line, InlineBox inline, int padX, Border margin, Border border, Border padding) {
        int padding_yoff = margin.top + border.top + padding.top;
        if (padding.right <= 0 && border.right <= 0) return;

        int ty = line.getBaseline() - inline.y - inline.height - padding_yoff + line.y;

        LineMetrics lm = FontUtil.getLineMetrics(c, inline);
        ty += (int) lm.getDescent();
        c.translate(padX, ty);
        //int old_width = inline.width;
        //int old_height = inline.height;
        //sets the inline borders to the current style, so border objects will exist
        //inline.height += margin.top + border.top + padding.top + padding.bottom + border.bottom + margin.bottom;
        //inline.width = padding.right + border.right + margin.right;
        if (padding.right > 0) {
            // paint the background
            Color background_color = BoxRendering.getBackgroundColor(c);
            if (background_color != null) {
                // skip transparent background
                if (!background_color.equals(BackgroundPainter.transparent)) {
                    Rectangle box = new Rectangle(inline.x,
                            inline.y + margin.top + border.top,
                            padding.right,
                            inline.height + padding.top + padding.bottom);
                    //TODO. make conf controlled Uu.p("filling a background");
                    c.getGraphics().setColor(background_color);
                    c.getGraphics().fillRect(box.x, box.y, box.width, box.height);
                }
            }
        }

        /*if (border.right > 0) {
            Rectangle bounds = new Rectangle(inline.x,
                    inline.y + margin.top,
                    inline.width - margin.right,
                    inline.height - margin.top - margin.bottom);
            BorderPainter.paint(bounds, BorderPainter.RIGHT + BorderPainter.TOP + BorderPainter.BOTTOM, c.getCurrentStyle(), c.getGraphics(), c.getCtx());
        }*/
        //inline.width = old_width;
        //inline.height = old_height;
        //c.translate(+padding_xoff, -ty);
        c.translate(-padX, -ty);

        InlineBorder ib = (InlineBorder) c.getInlineBorders().removeLast();
        for (Iterator i = c.getInlineBorders().iterator(); i.hasNext();) {
            ((InlineBorder) i.next()).paint(c,
                    line,
                    padX + inline.x,
                    padding.right + border.right,
                    BorderPainter.TOP + BorderPainter.BOTTOM);
        }

        ib.paint(c, line, padX + inline.x, padding.right + border.right, BorderPainter.RIGHT + BorderPainter.TOP + BorderPainter.BOTTOM);
    }

    /**
     * @param c       PARAM
     * @param line    PARAM
     * @param inline  PARAM
     * @param margin
     * @param border
     * @param padding
     */
    public static void paintMargin(Context c, LineBox line, InlineBox inline, int padX, int width, Color background_color, Border margin, Border border, Border padding) {
        if (width <= 0) return;
        // paint the background
        if (background_color == null) return;
        // skip transparent background
        if (background_color.equals(BackgroundPainter.transparent)) return;
        int padding_yoff = margin.top + border.top + padding.top;

        int ty = line.getBaseline() - inline.y - inline.height - padding_yoff + line.y;

        LineMetrics lm = FontUtil.getLineMetrics(c, inline);
        ty += (int) lm.getDescent();
        c.translate(padX, ty);
        //int old_width = inline.width;
        //int old_height = inline.height;
        //sets the inline borders to the current style, so border objects will exist
        //inline.height += margin.top + border.top + padding.top + padding.bottom + border.bottom + margin.bottom;
        //inline.width = padding.right + border.right + margin.right;
        // paint the actual background
        //can't use this, too uncontrolled: BoxRendering.paintBackground(c, inline);
        //Border margin = c.getCurrentStyle().getMarginWidth(c.getBlockFormattingContext().getWidth(), c.getBlockFormattingContext().getHeight());
        Rectangle box = new Rectangle(inline.x,
                inline.y + margin.top + border.top,
                width,
                inline.height + padding.top + padding.bottom);

        //TODO. make conf controlled Uu.p("filling a background");
        c.getGraphics().setColor(background_color);
        c.getGraphics().fillRect(box.x, box.y, box.width, box.height);

        //inline.width = old_width;
        //inline.height = old_height;
        //c.translate(+padding_xoff, -ty);
        c.translate(-padX, -ty);

        for (Iterator i = c.getInlineBorders().iterator(); i.hasNext();) {
            ((InlineBorder) i.next()).paint(c,
                    line,
                    inline.x + padX,
                    width,
                    BorderPainter.TOP + BorderPainter.BOTTOM);
        }
    }

    /**
     * Paint all of the inlines in this box. It recurses through each line, and
     * then each inline in each line, and paints them individually.
     *
     * @param c       PARAM
     * @param box     PARAM
     * @param restyle PARAM
     */
    static void paintInlineContext(Context c, Box box, boolean restyle) {
        //dummy style to make sure that text nodes don't get extra padding and such
        {
            LinkedList decorations = c.getDecorations();
            c.pushStyle(CascadedStyle.emptyCascadedStyle);
            //BlockBox block = (BlockBox)box;
            // translate into local coords
            // account for the origin of the containing box
            c.translate(box.x, box.y);
            // for each line box
            BlockBox block = null;
            if (box instanceof BlockBox) {//Why isn't it always a BlockBox? Because of e.g. Floats!
                block = (BlockBox) box;
            }

            CascadedStyle firstLineStyle = null;
            for (int i = 0; i < box.getChildCount(); i++) {
                if (i == 0 && block != null && block.firstLineStyle != null) {
                    firstLineStyle = block.firstLineStyle;
                }
                // get the line box
                paintLine(c, (LineBox) box.getChild(i), restyle, firstLineStyle, decorations);
                if (i == 0 && block != null && block.firstLineStyle != null) {
                    firstLineStyle = null;
                }
            }

            // translate back to parent coords
            c.translate(-box.x, -box.y);
            //pop dummy style
            c.popStyle();
        }
    }

    /**
     * paint all of the inlines on the specified line
     *
     * @param c           PARAM
     * @param line        PARAM
     * @param restyle     PARAM
     * @param decorations
     */
    static void paintLine(Context c, LineBox line, boolean restyle, CascadedStyle firstLineStyle, LinkedList decorations) {
        // get Xx and y
        int lx = line.x;
        int ly = line.y + line.getBaseline();

        LinkedList pushedStyles = null;
        if (firstLineStyle != null) {
            pushedStyles = new LinkedList();
            c.pushStyle(firstLineStyle);
        }

        // for each inline box
        for (int j = 0; j < line.getChildCount(); j++) {
            Box child = line.getChild(j);
            if (child.absolute) {
                LinkedList unpropagated = (LinkedList) decorations.clone();
                decorations.clear();
                paintAbsolute(c, child, restyle);
                decorations.addAll(unpropagated);
                continue;
            }

            InlineBox box = (InlineBox) child;
            paintInline(c, box, lx, ly, line, restyle, pushedStyles, decorations);
        }

        //do text decorations, all are still active
        ListIterator li = decorations.listIterator(0);
        while (li.hasNext()) {
            TextDecoration decoration = (TextDecoration) li.next();
            decoration.paint(c, line);
        }

        if (firstLineStyle != null) {
            for (int i = 0; i < pushedStyles.size(); i++) {
                IdentValue decoration = c.getCurrentStyle().getIdent(CSSName.TEXT_DECORATION);
                if (decoration != IdentValue.NONE) {
                    decorations.removeLast();//might have changed because of first line style
                }
                c.popStyle();
            }
            c.popStyle();//get rid of firstLineStyle
            //reinstitute the rest
            for (Iterator i = pushedStyles.iterator(); i.hasNext();) {
                c.pushStyle((CascadedStyle) i.next());
                IdentValue decoration = c.getCurrentStyle().getIdent(CSSName.TEXT_DECORATION);
                if (decoration != IdentValue.NONE) {
                    decorations.addLast(new TextDecoration(decoration, 0, c.getCurrentStyle().getColor(), FontUtil.getLineMetrics(c, null)));
                }
            }
        }
        if (c.debugDrawLineBoxes()) {
            GraphicsUtil.drawBox(c.getGraphics(), line, Color.blue);
        }
    }


    /**
     * Inlines are drawn vertically relative to the baseline of the containing
     * line box, not relative to the origin of the line. They *are* drawn
     * horizontally (Xx) relative to the origin of the containing line box
     * though
     *
     * @param c           PARAM
     * @param ib          PARAM
     * @param lx          PARAM
     * @param ly          PARAM
     * @param line        PARAM
     * @param restyle     PARAM
     * @param decorations
     */
    static void paintInline(Context c, InlineBox ib, int lx, int ly, LineBox line, boolean restyle, LinkedList pushedStyles, LinkedList decorations) {
        restyle = restyle || ib.restyle;//cascade it down
        ib.restyle = false;//reset
        int padX = 0;
        if (ib.pushstyles != null) {
            for (Iterator i = ib.pushstyles.iterator(); i.hasNext();) {
                CalculatedStyle style = c.getCurrentStyle();
                int parent_width = line.getParent().width;
                Border border = style.getBorderWidth(c.getCtx());
                //note: percentages here refer to width of containing block
                Border margin = style.getMarginWidth(parent_width, parent_width, c.getCtx());
                Border padding = style.getPaddingWidth(parent_width, parent_width, c.getCtx());
                Color background_color = BoxRendering.getBackgroundColor(c);
                paintMargin(c, line, ib, padX, margin.left, background_color, margin, border, padding);

                StylePush sp = (StylePush) i.next();
                CascadedStyle cascaded = c.getCss().getCascadedStyle(sp.getElement(), restyle);
                c.pushStyle(cascaded);
                if (pushedStyles != null) pushedStyles.addLast(cascaded);

                //Now we know that an inline element started here, handle borders and such
                Relative.translateRelative(c);
                style = c.getCurrentStyle();
                border = style.getBorderWidth(c.getCtx());
                //note: percentages here refer to width of containing block
                margin = style.getMarginWidth(parent_width, parent_width, c.getCtx());
                padding = style.getPaddingWidth(parent_width, parent_width, c.getCtx());
                //left padding for this inline element
                //paintLeftPadding takes the margin into account
                paintLeftPadding(c, line, ib, padX, margin, border, padding);
                padX += margin.left + border.left + padding.left;
                //text decoration?
                IdentValue decoration = c.getCurrentStyle().getIdent(CSSName.TEXT_DECORATION);
                if (decoration != IdentValue.NONE) {
                    decorations.addLast(new TextDecoration(decoration, ib.x + margin.left + border.left + padding.left, c.getCurrentStyle().getColor(), FontUtil.getLineMetrics(c, null)));
                }
            }
        }

        if (ib.floated) {
            LinkedList unpropagated = (LinkedList) decorations.clone();
            decorations.clear();
            paintFloat(c, ib, restyle);
            decorations.addAll(unpropagated);
            debugInlines(c, ib, lx, ly);
        } // Uu.p("paintInline: " + inline);
        else if (ib instanceof InlineBlockBox) {
            //no text-decorations on inline-block
            LinkedList restarted = new LinkedList();
            for (Iterator i = decorations.iterator(); i.hasNext();) {
                TextDecoration td = (TextDecoration) i.next();
                td.setEnd(ib.x);
                td.paint(c, line);
                i.remove();
                restarted.addLast(td.getRestarted(ib.x + ib.width));
            }
            decorations.clear();
            c.pushStyle(c.getCss().getCascadedStyle(ib.element, restyle));
            c.translate(line.x,
                    line.y +
                    (line.getBaseline() -
                    VerticalAlign.getBaselineOffset(c, line, ib) -
                    ib.height));
            BoxRendering.paint(c, ib, true, restyle);
            c.translate(-line.x,
                    -(line.y +
                    (line.getBaseline() -
                    VerticalAlign.getBaselineOffset(c, line, ib) -
                    ib.height)));
            debugInlines(c, ib, lx, ly);
            c.popStyle();
            decorations.addAll(restarted);
        } else {

            InlineTextBox inline = (InlineTextBox) ib;

            c.updateSelection(inline);

            // calculate the Xx and y relative to the baseline of the line (ly) and the
            // left edge of the line (lx)
            int iy = ly - VerticalAlign.getBaselineOffset(c, line, inline);
            int ix = lx + inline.x;//TODO: find the right way to work this out

            // account for padding
            // Uu.p("adjusted inline by: " + inline.totalLeftPadding());
            // Uu.p("inline = " + inline);
            // Uu.p("padding = " + inline.padding);

            // JMM: new adjustments to move the text to account for horizontal insets
            //int padding_xoff = inline.totalLeftPadding(c.getCurrentStyle());
            c.translate(padX, 0);
            LineMetrics lm = FontUtil.getLineMetrics(c, inline);
            paintBackground(c, line, inline, padX, lm);

            paintSelection(c, inline, lx, ly, lm);
            paintText(c, ix, iy, inline, lm);
            c.translate(-padX, 0);
            debugInlines(c, inline, lx, ly);
        }

        padX = ib.width - ib.rightPadding;

        if (ib.popstyles != 0) {
            for (int i = 0; i < ib.popstyles; i++) {
                //end text decoration?
                IdentValue decoration = c.getCurrentStyle().getIdent(CSSName.TEXT_DECORATION);
                if (decoration != IdentValue.NONE) {
                    TextDecoration td = (TextDecoration) decorations.getLast();
                    td.setEnd(ib.x + padX);
                    td.paint(c, line);
                    decorations.removeLast();
                }
                //right padding for this inline element
                CalculatedStyle style = c.getCurrentStyle();
                int parent_width = line.getParent().width;
                Border border = style.getBorderWidth(c.getCtx());
                //note: percentages here refer to width of containing block
                Border margin = style.getMarginWidth(parent_width, parent_width, c.getCtx());
                Border padding = style.getPaddingWidth(parent_width, parent_width, c.getCtx());
                paintRightPadding(c, line, ib, padX, margin, border, padding);
                padX += padding.right + border.right;
                Relative.untranslateRelative(c);
                c.popStyle();
                if (pushedStyles != null) pushedStyles.removeLast();
                style = c.getCurrentStyle();
                border = style.getBorderWidth(c.getCtx());
                //note: percentages here refer to width of containing block
                margin = style.getMarginWidth(parent_width, parent_width, c.getCtx());
                padding = style.getPaddingWidth(parent_width, parent_width, c.getCtx());
                Color background_color = BoxRendering.getBackgroundColor(c);
                paintMargin(c, line, ib, padX, margin.right, background_color, margin, border, padding);
                padX += margin.right;
            }
        }
    }

    /**
     * Description of the Method
     *
     * @param c      PARAM
     * @param inline PARAM
     * @param lx     PARAM
     * @param ly     PARAM
     */
    static void debugInlines(Context c, InlineBox inline, int lx, int ly) {
        if (c.debugDrawInlineBoxes()) {
            GraphicsUtil.draw(c.getGraphics(), new Rectangle(lx + inline.x + 1, ly + inline.y + 1 - inline.height,
                    inline.width - 2, inline.height - 2), Color.green);
        }
    }


    /**
     * Description of the Method
     *
     * @param c       PARAM
     * @param inline  PARAM
     * @param restyle PARAM
     */
    static void paintAbsolute(Context c, Box inline, boolean restyle) {
        restyle = restyle || inline.restyle;
        inline.restyle = false;//reset
        // Uu.p("paint absolute: " + inline);
        BoxRendering.paint(c, inline, false, restyle);
    }


    /**
     * Description of the Method
     *
     * @param c       PARAM
     * @param inline  PARAM
     * @param restyle PARAM
     */
    static void paintFloat(Context c, InlineBox inline, boolean restyle) {
        restyle = restyle || inline.restyle;//should already have been done, but it can't hurt
        inline.restyle = false;//reset
        // Uu.p("painting a float: " + inline);
        Rectangle oe = c.getExtents();
        c.setExtents(new Rectangle(oe.x, 0, oe.width, oe.height));
        int xoff = 0;
        int yoff = 0;//line.y + ( line.baseline - inline.height );// + inline.y;
        c.translate(xoff, yoff);
        BoxRendering.paint(c, inline, false, restyle);
        c.translate(-xoff, -yoff);
        c.setExtents(oe);
    }
}


