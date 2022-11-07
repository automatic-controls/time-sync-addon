/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.timesync;
import javax.servlet.http.*;
public class Config extends ServletBase {
  @Override public void exec(HttpServletRequest req, HttpServletResponse res) throws Throwable {
    final String expr = req.getParameter("expr");
    if (expr==null){
      final String currentExpr = Initializer.getCronExpression();
      res.setContentType("text/html");
      res.getWriter().print(getHTML(req).replace("__CRON_EXPR__",currentExpr==null?"":currentExpr).replace("__NEXT_SYNC__", Initializer.getNextString()));
    }else{
      Initializer.setCronExpression(expr);
      res.setContentType("text/plain");
      res.getWriter().print(Initializer.getNextString());
    }
  }
}