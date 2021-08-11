package ap.mailo;

import androidx.appcompat.widget.Toolbar;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.Root;
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
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;

import ap.mailo.main.EntryActivity;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTests extends BaseUITest
{
    @Rule
    public ActivityScenarioRule<EntryActivity> activityScenarioRule
            = new ActivityScenarioRule<>(EntryActivity.class);


    @Test
    public void openMenuAndChangeMailFolder() throws InterruptedException {

        onView(allOf(isAssignableFrom(ImageButton.class),
                withParent(isAssignableFrom(Toolbar.class))))
                .perform(click());

        Thread.sleep(2000);

        onView(allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.navigationView),
                                        0)),
                        2),
                        isDisplayed()))
            .perform(click());

        Thread.sleep(2000);
        
        onView(withId(R.id.FolderTitle)).check(matches(withText("Drafts")));
    }

    @Test
    public void composeCorrectMailAndSend() throws InterruptedException {

        onView(withId(R.id.fab)).perform(click());

        onView(withId(R.id.writeSubject)).perform(typeText("Some title"));
        onView(withId(R.id.writeTo)).perform(typeText("example@example.com"));
        onView(withId(R.id.writeContent)).perform(typeText("Some content"));

        closeSoftKeyboard();

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(2000);

        //isToastMessageDisplayed(R.string.succes_sending); Check is not working in SDK31
        onView(withId(R.id.FolderTitle)).check(matches(isDisplayed()));
    }

    @Test
    public void composeIncorrectMail() throws InterruptedException {

        onView(withId(R.id.fab)).perform(click());

        onView(withId(R.id.writeSubject)).perform(typeText("Some title"));
        onView(withId(R.id.writeTo)).perform(typeText("example@"));
        onView(withId(R.id.writeContent)).perform(typeText("Some content"));

        closeSoftKeyboard();

        onView(withId(R.id.fab)).check(matches(isNotEnabled()));

        onView(withId(R.id.fab)).perform(click());

        Thread.sleep(2000);

        onView(withId(R.id.FolderTitle)).check(doesNotExist());
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
}
