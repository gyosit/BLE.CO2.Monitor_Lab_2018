function doGet(req) {
  var target = req.parameters.target[0]
  var val = req.parameters.val[0];
  var time = Utilities.formatDate(new Date(), "JST", "yyyy/MM/dd HH:mm:ss")
  Logger.log(val);
  var vals = val.split(",");
  var Targetspread = SpreadsheetApp.openById("102jZsYBK-udfBA1Lnx9tisrPiWTIhNW0Mu4VSYmpiZ4");
  var Targetsheet = Targetspread.getSheetByName(target);
  if(Targetsheet == null){
    var activesheet = Targetspread.getActiveSheet();
    Targetsheet = Targetspread.insertSheet(target);
    Targetsheet = Targetspread.getSheetByName(target);
  }
  Targetdat = Targetsheet.getDataRange().getValues();
  var tmpdata = [time];
  for(var i=0;i<vals.length;i++)
      tmpdata.push(vals[i]);
  if(Targetdat[0][0].length != 0){
    Targetdat.push(tmpdata);
  }else{
    Targetdat = [tmpdata];
  }
  Logger.log(Targetdat);
  Targetsheet.getRange(1, 1, Targetdat.length, Targetdat[0].length).setValues(Targetdat);
  
  return ContentService.createTextOutput("Sucessful");
}