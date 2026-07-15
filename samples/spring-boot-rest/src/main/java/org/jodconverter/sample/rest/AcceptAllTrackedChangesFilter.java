package org.jodconverter.sample.rest;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.XComponentContext;
import org.jodconverter.core.office.OfficeContext;
import org.jodconverter.local.filter.Filter;
import org.jodconverter.local.filter.FilterChain;
import org.jodconverter.local.office.LocalOfficeContext;
import org.jodconverter.local.office.utils.Lo;
import org.jodconverter.local.office.utils.Write;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Filter} that accepts all tracked changes (redlines) in a Writer document before it is
 * passed further down the conversion chain (e.g. exported to PDF).
 *
 * <p>By default LibreOffice exports a document with its tracked changes preserved, so the resulting
 * PDF may still contain insertion/deletion markup. This filter applies all revisions in memory by
 * dispatching the {@code .uno:AcceptAllTrackedChanges} command, producing the clean "final" view.
 *
 * <p>Only Writer (text) documents are affected; other document types pass through untouched.
 */
public class AcceptAllTrackedChangesFilter implements Filter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AcceptAllTrackedChangesFilter.class);

  private static final String ACCEPT_ALL_TRACKED_CHANGES = ".uno:AcceptAllTrackedChanges";

  @Override
  public void doFilter(
      final OfficeContext context, final XComponent document, final FilterChain chain)
      throws Exception {

    if (Write.isText(document)) {
      LOGGER.debug("Applying AcceptAllTrackedChangesFilter to text document");
      acceptAllTrackedChanges(context, document);
    } else {
      LOGGER.debug("Skipping AcceptAllTrackedChangesFilter (document is not a text document)");
    }

    // Continue the conversion chain.
    chain.doFilter(context, document);
  }

  private static void acceptAllTrackedChanges(
      final OfficeContext context, final XComponent document) {

    final XComponentContext componentContext =
        ((LocalOfficeContext) context).getComponentContext();
    if (componentContext == null) {
      LOGGER.warn("No component context available; cannot accept tracked changes");
      return;
    }

    final XModel model = Lo.qi(XModel.class, document);
    final XController controller = model.getCurrentController();
    if (controller == null) {
      LOGGER.warn("No controller available; cannot accept tracked changes");
      return;
    }

    final XFrame frame = controller.getFrame();
    if (frame == null) {
      LOGGER.warn("No frame available; cannot accept tracked changes");
      return;
    }

    final XDispatchProvider dispatchProvider = Lo.qi(XDispatchProvider.class, frame);
    final XDispatchHelper dispatchHelper =
        Lo.createInstance(
            componentContext, XDispatchHelper.class, "com.sun.star.frame.DispatchHelper");
    if (dispatchHelper == null) {
      LOGGER.warn("Could not create DispatchHelper; cannot accept tracked changes");
      return;
    }

    dispatchHelper.executeDispatch(
        dispatchProvider, ACCEPT_ALL_TRACKED_CHANGES, "", 0, new PropertyValue[0]);
  }
}
