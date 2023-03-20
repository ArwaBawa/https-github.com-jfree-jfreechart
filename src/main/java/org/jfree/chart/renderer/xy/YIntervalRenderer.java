/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-present, by David Gilbert and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 * ----------------------
 * YIntervalRenderer.java
 * ----------------------
 * (C) Copyright 2002-present, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.chart.renderer.xy;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Objects;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * A renderer that draws a line connecting the start and end Y values for an
 * {@link XYPlot}.  The example shown here is generated by the
 * {@code YIntervalRendererDemo1.java} program included in the JFreeChart
 * demo collection:
 * <br><br>
 * <img src="doc-files/YIntervalRendererSample.png"
 * alt="YIntervalRendererSample.png">
 */
public class YIntervalRenderer extends AbstractXYItemRenderer
        implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -2951586537224143260L;

    /**
     * An additional item label generator.  If this is non-null, the item
     * label generated will be displayed near the lower y-value at the
     * position given by getNegativeItemLabelPosition().
     */
    private XYItemLabelGenerator additionalItemLabelGenerator;

    /**
     * The default constructor.
     */
    public YIntervalRenderer() {
        super();
        this.additionalItemLabelGenerator = null;
    }

    /**
     * Returns the generator for the item labels that appear near the lower
     * y-value.
     *
     * @return The generator (possibly {@code null}).
     *
     * @see #setAdditionalItemLabelGenerator(XYItemLabelGenerator)
     */
    public XYItemLabelGenerator getAdditionalItemLabelGenerator() {
        return this.additionalItemLabelGenerator;
    }

    /**
     * Sets the generator for the item labels that appear near the lower y-value
     * and calls {@link #fireChangeEvent()}. If this is set to {@code null}, no
     * item labels will be drawn.
     *
     * @param generator  the generator ({@code null} permitted).
     *
     * @see #getAdditionalItemLabelGenerator()
     */
    public void setAdditionalItemLabelGenerator(
            XYItemLabelGenerator generator) {
        this.additionalItemLabelGenerator = generator;
        fireChangeEvent();
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset ({@code null} permitted).
     *
     * @return The range ({@code null} if the dataset is {@code null} or empty).
     */
    @Override
    public Range findRangeBounds(XYDataset dataset) {
        return findRangeBounds(dataset, true);
    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects information about the drawing.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairState  crosshair information for the plot
     *                        ({@code null} permitted).
     * @param pass  the pass index (ignored here).
     */
    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(series, item)) {
            return;
        }

        // setup for collecting optional entity info...
        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;

        double x = intervalDataset.getXValue(series, item);
        double yLow   = intervalDataset.getStartYValue(series, item);
        double yHigh  = intervalDataset.getEndYValue(series, item);

        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        double xx = domainAxis.valueToJava2D(x, dataArea, xAxisLocation);
        double yyLow = rangeAxis.valueToJava2D(yLow, dataArea, yAxisLocation);
        double yyHigh = rangeAxis.valueToJava2D(yHigh, dataArea, yAxisLocation);

        Paint p = getItemPaint(series, item);
        Stroke s = getItemStroke(series, item);

        Line2D line = null;
        Shape shape = getItemShape(series, item);
        Shape top = null;
        Shape bottom = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            line = new Line2D.Double(yyLow, xx, yyHigh, xx);
            top = ShapeUtils.createTranslatedShape(shape, yyHigh, xx);
            bottom = ShapeUtils.createTranslatedShape(shape, yyLow, xx);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            line = new Line2D.Double(xx, yyLow, xx, yyHigh);
            top = ShapeUtils.createTranslatedShape(shape, xx, yyHigh);
            bottom = ShapeUtils.createTranslatedShape(shape, xx, yyLow);
        } else {
            throw new IllegalStateException();
        }
        g2.setPaint(p);
        g2.setStroke(s);
        g2.draw(line);

        g2.fill(top);
        g2.fill(bottom);

        // for item labels, we have a special case because there is the
        // possibility to draw (a) the regular item label near to just the
        // upper y-value, or (b) the regular item label near the upper y-value
        // PLUS an additional item label near the lower y-value.
        if (isItemLabelVisible(series, item)) {
            drawItemLabel(g2, orientation, dataset, series, item, xx, yyHigh,
                    false);
            drawAdditionalItemLabel(g2, orientation, dataset, series, item,
                    xx, yyLow);
        }

        // add an entity for the item...
        Shape hotspot = ShapeUtils.createLineRegion(line, 4.0f);
        if (entities != null && hotspot.intersects(dataArea)) {
            addEntity(entities, hotspot, dataset, series, item, 0.0, 0.0);
        }

    }

    /**
     * Draws an item label.
     *
     * @param g2  the graphics device.
     * @param orientation  the orientation.
     * @param dataset  the dataset.
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param x  the x coordinate (in Java2D space).
     * @param y  the y coordinate (in Java2D space).
     */
    private void drawAdditionalItemLabel(Graphics2D g2,
            PlotOrientation orientation, XYDataset dataset, int series,
            int item, double x, double y) {

        if (this.additionalItemLabelGenerator == null) {
            return;
        }

        Font labelFont = getItemLabelFont(series, item);
        Paint paint = getItemLabelPaint(series, item);
        g2.setFont(labelFont);
        g2.setPaint(paint);
        String label = this.additionalItemLabelGenerator.generateLabel(dataset,
                series, item);

        ItemLabelPosition position = getNegativeItemLabelPosition(series, item);
        Point2D anchorPoint = calculateLabelAnchorPoint(
                position.getItemLabelAnchor(), x, y, orientation);
        TextUtils.drawRotatedString(label, g2,
                (float) anchorPoint.getX(), (float) anchorPoint.getY(),
                position.getTextAnchor(), position.getAngle(),
                position.getRotationAnchor());
    }

    /**
     * Tests this renderer for equality with an arbitrary object.
     *
     * @param obj  the object ({@code null} permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof YIntervalRenderer)) {
            return false;
        }
        YIntervalRenderer that = (YIntervalRenderer) obj;
        if (!Objects.equals(this.additionalItemLabelGenerator,
                that.additionalItemLabelGenerator)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a clone of the renderer.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException  if the renderer cannot be cloned.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
