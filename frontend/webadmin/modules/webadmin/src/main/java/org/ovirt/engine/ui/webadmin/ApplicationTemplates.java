package org.ovirt.engine.ui.webadmin;

import org.ovirt.engine.ui.common.CommonApplicationTemplates;

import com.google.gwt.safehtml.shared.SafeHtml;

public interface ApplicationTemplates extends CommonApplicationTemplates {

    /**
     * Creates a progress bar template.
     *
     * @param progress
     *            Progress value in percent.
     * @param text
     *            Text to show within the progress bar.
     */
    @Template("<div class='engine-progress-box'>" +
            "<div style='background: {2}; width: {0}%; height: 100%'></div>" +
            "<div class='engine-progress-text'>{1}</div></div>")
    SafeHtml progressBar(int progress, String text, String color);

    /**
     * Creates a tree-item HTML
     *
     * @param imageHtml
     *            the image HTML
     * @param text
     *            the node title
     * @return
     */
    @Template("<span style='position: relative; bottom: 1px;'>{0}</span>" +
            "<span style='position: relative; bottom: 7px;'>{1}</span>")
    SafeHtml treeItem(SafeHtml imageHtml, String text);

    /**
     * Creates a bookmark-item HTML
     *
     * @param text
     *            the bookmark title
     */
    @Template("<span id='{0}' style='display: inline-block; padding: 5px;'>{1}</span>")
    SafeHtml bookmarkItem(String id, String text);

    /**
     * Creates a tag-item HTML
     *
     * @param imageHtml
     *            the image HTML
     * @param text
     *            the node title
     * @return
     */
    @Template("<span style='position: relative; border: 1px solid {3}; " +
            "bottom: 4px; padding: 0 3px; margin: 0 1px;  white-space: nowrap; background-color: {2};'>" +
            "<span style='position: relative; top: 1px;'>{0}</span> {1}</span>")
    SafeHtml tagItem(SafeHtml imageHtml, String text, String backgroundColor, String borderColor);

    /**
     * Creates a tag-button HTML
     *
     * @param imageHtml
     *            the image HTML
     * @return
     */
    @Template("<span style='position: relative; border: 1px solid {2}; visibility: {3};" +
            " bottom: 4px; padding: 0 3px; background-color: {1};'>{0}</span>")
    SafeHtml tagButton(SafeHtml imageHtml, String backgroundColor, String borderColor, String visibility);

    @Template("<span style='position: relative; white-space: nowrap;'><span>{0}</span>{1} Alerts</span>")
    SafeHtml alertFooterHeader(SafeHtml imageHtml, int alertCount);

    @Template("<table cellspacing='0' cellpadding='0'><tr>"
            +
            "<td><div style='background: url({2}); width: 4px; height: 20px; float:left;'></div>"
            +
            "<div class='{5}' style='background: url({3}) repeat-x; white-space: nowrap; height: 20px; line-height: 20px; padding-right: 4px;'>"
            +
            "<span style='vertical-align: middle; margin-right: 3px; line-height: 20px;'>{0}</span>{1}</div></td>" +
            "<td><div style='background: url({4}); width: 4px; height: 20px; float: right;'></div></td>" +
            "</tr></table>")
    SafeHtml alertEventButton(SafeHtml image, String text, String start, String stretch,
            String end, String contentStyleName);

    @Template("<div style=\"text-align: center; padding-top: 6px;\">{0}{1}</div>")
    SafeHtml statusWithAlertTemplate(SafeHtml statusImage, SafeHtml alertImage);

    @Template("<div style=\"text-align: center; padding-top: 6px;\">{0}</div>")
    SafeHtml statusTemplate(SafeHtml statusImage);

    @Template("<button type='button' tabindex='-1' style='float: right; height: 20px;'>"
            +
            "<span style='position: relative; left: 0px; top: -5px; width: 100%; font-family: arial; font-size: 10px;'>{0}</span></button>")
    SafeHtml actionButtonText(String title);

    @Template("<button type='button' tabindex='-1' style='background: url({0}) no-repeat; white-space: nowrap; height: 20px; width: 20px; line-height: 20px; float: right;'></button>")
    SafeHtml actionButtonImage(String image);

    @Template("<span style=\"top: -3px; position: relative;\">{0}</span>")
    SafeHtml textForCheckBoxHeader(String text);

    @Template("<span style=\"top: -2px; position: relative;\">{0}</span>")
    SafeHtml textForCheckBox(String text);

    @Template("{0} <span style='font-weight:bold; color: red;'>{1}</span>")
    SafeHtml blackRedBold(String black, String redBold);

    @Template("{0} <span style='font-weight:bold;'>{1}</span> {2}")
    SafeHtml middleBold(String start, String middle, String end);

    @Template("<span><span style='position: relative; margin-left: 20px; display: inline-block; vertical-align: top; height: 14px; line-height: 14px;'>{0}</span>"
            + "<span style='position: relative; margin-left: 3px; margin-right: 3px; white-space: nowrap; height: 14px; line-height: 14px;'>{1}</span></span>")
    SafeHtml imageTextSetupNetworkUsage(SafeHtml image, String text);

    @Template("<span><span style='position: relative; display: inline-block; vertical-align: top; height: 14px; line-height: 14px;'>{0}</span>"
            + "<span style='position: relative; margin-left: 3px; margin-right: 3px; white-space: nowrap; height: 14px; line-height: 14px;'>{1}</span></span>")
    SafeHtml imageTextSetupNetwork(SafeHtml image, String text);

    @Template("<div style='font-weight:bold; border-bottom-style:solid; border-bottom-width:1px; border-top-style:solid; border-top-width:1px; width:100%;'>{0}</div> ")
    SafeHtml titleSetupNetworkTooltip(String title);

    @Template("<I>{0}<BR>{1}</I>")
    SafeHtml italicTwoLines(String firstLine, String secondLine);

    @Template("<div style='width: {0}; font-style: italic;'>{1}</div>")
    SafeHtml italicFixedWidth(String pxWidth, String text);

    @Template("<div style='background: url({0}) no-repeat; height: {1}px; width: {2}px;'></div>")
    SafeHtml image(String url, int height, int width);

    @Template("<div style='line-height: 100%; text-align: center; vertical-align: middle;'>{0}</div>")
    SafeHtml image(SafeHtml statusImage);
}
