import Foundation
import Vision

guard CommandLine.arguments.count == 2 else {
    FileHandle.standardError.write(Data("Usage: swift check-ios-startup.swift <screenshot>\n".utf8))
    exit(2)
}

let screenshot = URL(fileURLWithPath: CommandLine.arguments[1])
let request = VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.recognitionLanguages = ["en-GB"]
try VNImageRequestHandler(url: screenshot, options: [:]).perform([request])

let text = (request.results ?? [])
    .compactMap { $0.topCandidates(1).first?.string }
    .joined(separator: " ")
    .lowercased()

// Require text from the app's first-run UI. SpringBoard also shows the app name.
guard text.contains("make today yours"),
      text.contains("record my runs") else {
    FileHandle.standardError.write(Data("First-run coaching screen is not visible.\n".utf8))
    exit(1)
}
