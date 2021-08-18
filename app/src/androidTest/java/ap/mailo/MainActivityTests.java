package ap.mailo;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.Root;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.core.internal.deps.guava.base.Preconditions.checkNotNull;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webContent;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;

import ap.mailo.main.EntryActivity;
import ap.mailo.store.MessageHeader;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTests extends BaseUITest
{
    @Rule
    public ActivityScenarioRule<EntryActivity> activityScenarioRule
            = new ActivityScenarioRule<>(EntryActivity.class);

    @Test
    public void openMenuAndChangeMailFolder() throws InterruptedException {
        changeMessageFolderIntoABC();

        Thread.sleep(10000);
        
        onView(withId(R.id.FolderTitle)).check(matches(withText("ABC")));
    }

    @Test
    public void composeCorrectMailAndSend() throws InterruptedException {
        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.writeSubject)).perform(typeText("Some title"));
        onView(withId(R.id.writeTo)).perform(typeText("example@example.com"));
        onView(withId(R.id.writeContent)).perform(typeText("Some content"));

        closeSoftKeyboard();

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(5000);

        //isToastMessageDisplayed(R.string.succes_sending); Check is not working in SDK31
        onView(withId(R.id.FolderTitle)).check(matches(isDisplayed()));
    }

    @Test
    public void composeIncorrectMail() throws InterruptedException {
        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.writeSubject)).perform(typeText("Some title"));
        onView(withId(R.id.writeTo)).perform(typeText("example@"));
        onView(withId(R.id.writeContent)).perform(typeText("Some content"));

        closeSoftKeyboard();

        onView(withId(R.id.fab)).check(matches(isNotEnabled()));

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.FolderTitle)).check(doesNotExist());
    }

    @Test
    public void checkIfMessageIsCorrectlyDisplayed() throws InterruptedException {
        changeMessageFolderIntoABC();

        Thread.sleep(10000);

        onView(withId(R.id.FolderTitle)).check(matches(withText("ABC")));

        onView(withId(R.id.messages_swipe_refresh)).perform(swipeDown());

        Thread.sleep(5000);

        openFirstMessageInFolder();

        Thread.sleep(5000);

        checkTestMessage();
    }

    @Test
    public void checkIfReplyInfoIsCopiedAndSendCorrectly() throws InterruptedException {
        changeMessageFolderIntoABC();

        Thread.sleep(5000);

        onView(withId(R.id.FolderTitle)).check(matches(withText("ABC")));

        onView(withId(R.id.messages_swipe_refresh)).perform(swipeDown());

        Thread.sleep(5000);

        openFirstMessageInFolder();

        Thread.sleep(5000);

        checkTestMessage();

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.writeSubject)).check(matches(withText(containsString("Re: Test"))));
        onView(withId(R.id.writeContent)).perform(typeText("Some content"));
        onView(withId(R.id.writeTo)).check(matches(withText(containsString("porowski126@wp.pl"))));

        closeSoftKeyboard();

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(10000);

        //isToastMessageDisplayed(R.string.succes_sending); Check is not working in SDK31
        onView(withId(R.id.FolderTitle)).check(matches(isDisplayed()));
    }

    private void changeMessageFolderIntoABC() throws InterruptedException {
        onView(allOf(isAssignableFrom(ImageButton.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .perform(click());

        Thread.sleep(5000);

        onView(allOf(childAtPosition(
                allOf(withId(R.id.design_navigation_view),
                        childAtPosition(
                                withId(R.id.navigationView),
                                0)),
                1),
                isDisplayed()))
                .perform(click());
    }

    private void openFirstMessageInFolder() {
        ViewInteraction recyclerView = onView(
                allOf(withId(R.id.messages_recyclerview),
                        childAtPosition(
                                withClassName(is("androidx.constraintlayout.widget.ConstraintLayout")),
                                1)));
        recyclerView.perform(actionOnItemAtPosition(0, click()));
    }

    private void checkTestMessage() {
        onView(withId(R.id.readSubject)).check(matches(withText(containsString("Test"))));
        onWebView(withId(R.id.readBody))
                .forceJavascriptEnabled()
                .withElement(findElement(Locator.ID, "content"))
                .check(webMatches(getText(), containsString("Test")));
        onView(withId(R.id.readFrom)).check(matches(withText(containsString("porowski126@wp.pl"))));
        onView(withId(R.id.readTo)).check(matches(withText(containsString("example@example.com"))));
    }

    //Matchers
        //Toast helpers
        public static Matcher<Root> isToast() {
            return new ToastMatcher();
        }

        public void isToastMessageDisplayed(int textId) {
            onView(withText(textId)).inRoot(isToast()).check(matches(isDisplayed()));
        }

        //Child at position helpers
        private static Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {

            return new TypeSafeMatcher<View>() {
                @Override
                public void describeTo(Description description) {
                    description.appendText("Child at position " + position + " in parent ");
                    parentMatcher.describeTo(description);
                }

                @Override
                public boolean matchesSafely(View view) {
                    ViewParent parent = view.getParent();
                    return parent instanceof ViewGroup && parentMatcher.matches(parent)
                            && view.equals(((ViewGroup) parent).getChildAt(position));
                }
            };
        }

        //RecyclerView child helper
        public static Matcher<View> atPosition(final int position, @NonNull final Matcher<View> itemMatcher) {
            checkNotNull(itemMatcher);
            return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
                @Override
                public void describeTo(Description description) {
                    description.appendText("has item at position " + position + ": ");
                    itemMatcher.describeTo(description);
                }

                @Override
                protected boolean matchesSafely(final RecyclerView view) {
                    RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                    if (viewHolder == null) {
                        // has no item on such position
                        return false;
                    }
                    return itemMatcher.matches(viewHolder.itemView);
                }
            };
        }
}
