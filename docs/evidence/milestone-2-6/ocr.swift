import Foundation
import Vision
import AppKit
let url=URL(fileURLWithPath:CommandLine.arguments[1])
let request=VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.usesLanguageCorrection = false
request.recognitionLanguages = ["en-US"]
try VNImageRequestHandler(url:url).perform([request])
let rows=(request.results ?? []).compactMap { r -> [String:Any]? in
 guard let c=r.topCandidates(1).first else {return nil}
 return ["text":c.string,"confidence":c.confidence,"x":r.boundingBox.midX,"y":1-r.boundingBox.midY]
}
let data=try JSONSerialization.data(withJSONObject:rows,options:[.sortedKeys])
print(String(data:data,encoding:.utf8)!)
