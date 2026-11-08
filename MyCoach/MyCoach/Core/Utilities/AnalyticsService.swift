import Foundation

enum AnalyticsEvent: String, Sendable { case firstGeneration, paywallViewed, trialStarted, feedbackGiven }
protocol AnalyticsService: Sendable { func track(_ event: AnalyticsEvent) }
struct NoOpAnalytics: AnalyticsService { func track(_ event: AnalyticsEvent) {} }
