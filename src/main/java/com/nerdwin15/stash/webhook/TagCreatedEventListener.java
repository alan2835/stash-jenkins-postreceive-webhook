package com.nerdwin15.stash.webhook;

import com.atlassian.event.api.EventListener;
import com.atlassian.bitbucket.event.tag.TagCreatedEvent;
import com.nerdwin15.stash.webhook.service.SettingsService;
import com.nerdwin15.stash.webhook.service.eligibility.EligibilityFilterChain;
import com.nerdwin15.stash.webhook.service.eligibility.EventContext;

/**
 * Event listener that listens to PullRequestRescopedEvent events.
 *
 * @author Alan Hamrick (alan28335)
 */
public class TagCreatedEventListener {

  private final EligibilityFilterChain filterChain;
  private final Notifier notifier;
  private final SettingsService settingsService;

  /**
   * Construct a new instance.
   * @param filterChain The filter chain to test for eligibility
   * @param notifier The notifier service
   * @param settingsService Service to be used to get the Settings
   */
  public TagCreatedEventListener(EligibilityFilterChain filterChain,
    Notifier notifier, SettingsService settingsService) {
    this.filterChain = filterChain;
    this.notifier = notifier;
    this.settingsService = settingsService;
  }

  /**
   * Event listener that is notified of tag creation events
   * @param event The tag created event
   */
  @EventListener
  public void onTagCreated(TagCreatedEvent event) {
    handleEvent(event);
  }

  /**
   * Actually handles the event that was triggered.
   * (Made protected to make unit testing easier)
   * @param event The event to be handled
   */
  protected void handleEvent(TagCreatedEvent event) {
    if (settingsService.getSettings(event.getRepository()) == null) {
      return;
    }

    StringBuilder strRefBuilder = new StringBuilder();
    strRefBuilder.append("pr/");
    strRefBuilder.append(event.getTag().getId());
    strRefBuilder.append("/from");
    String strRef = strRefBuilder.toString();
    String strSha1 = event.getTag().getLatestCommit();
    String targetBranch = event.getTag().getDisplayId();

    EventContext context = new EventContext(event,
      event.getRepository(),
      event.getUser().getName());

    if (filterChain.shouldDeliverNotification(context))
      notifier.notifyBackground(context.getRepository(), strRef, strSha1, targetBranch);
  }

}
